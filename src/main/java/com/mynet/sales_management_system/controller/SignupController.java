package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.UserRegistrationRequest;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.service.UserService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignupController {

    private final UserService userService;
    private final CompanyRepository companyRepository;

    /**
     * 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage(Model model) {
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
            BindingResult bindingResult) {
        try {
            // Validation 에러 체크 (SQL 인젝션 차단!)
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
}