package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.UserRegistrationRequest;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.service.UserService;
import com.mynet.sales_management_system.service.GeoLocationService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.BindingResult;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignupController {

    private final UserService userService;
    private final CompanyRepository companyRepository;
    private final GeoLocationService geoLocationService;

    /**
     * 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage(Model model, HttpServletRequest request) {
        // ✅ 한국 IP 체크
        String clientIP = getClientIP(request);
        if (!geoLocationService.isKoreanIP(clientIP)) {
            log.warn("외국 IP 회원가입 페이지 접근 차단: {} ({})",
                    clientIP, geoLocationService.getCountryInfo(clientIP));
            model.addAttribute("errorMessage", "이 서비스는 대한민국에서만 이용 가능합니다.");
            return "error/403"; // 403 에러 페이지로
        }

        List<Company> companies = companyRepository.findAll();
        model.addAttribute("companies", companies);
        return "signup";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/signup")
    @ResponseBody
    public String processSignup(@Valid @ModelAttribute UserRegistrationRequest request,
            BindingResult bindingResult, HttpServletRequest httpRequest) {
        try {

            // 한국 IP 체크
            String clientIP = getClientIP(httpRequest);
            if (!geoLocationService.isKoreanIP(clientIP)) {
                log.warn("외국 IP 회원가입 시도 차단: {} ({})",
                        clientIP, geoLocationService.getCountryInfo(clientIP));
                return "error:이 서비스는 대한민국에서만 이용 가능합니다.";
            }

            // Validation 에러 체크 (SQL 인젝션 차단)
            if (bindingResult.hasErrors()) {
                String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
                log.warn("회원가입 validation 실패: {}", errorMsg);
                return "error:" + errorMsg;
            }

            // 비밀번호 확인
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return "error:비밀번호가 일치하지 않습니다.";
            }

            // 마이넷 회사 선택 불허
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

            if (company.getIsMynet()) {
                return "error:마이넷 계정은 관리자만 생성할 수 있습니다.";
            }

            // USER 권한으로 고정
            userService.createUser(request.getUsername(), request.getPassword(),
                    request.getCompanyId(), Role.USER, false);
            log.info("새 사용자 가입: {}", request.getUsername());

            return "success";
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return "error:" + e.getMessage();
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}