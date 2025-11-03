// AuthController.java - 인증 관련 컨트롤러
package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 인증 및 메인 페이지 컨트롤러
 * - 로그인 후 사용자 타입별 리다이렉트
 * - 대시보드 페이지 라우팅
 */
@Controller
@Slf4j
public class AuthController {

    /**
     * 루트 페이지 - 로그인 페이지로 리다이렉트
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 대시보드 - 로그인 후 메인 페이지
     * 사용자 타입에 따라 적절한 페이지로 리다이렉트
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        // 사용자 정보를 모델에 추가
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("companyName", userDetails.getCompanyName());

        // 사용자 타입별 리다이렉트
        if (userDetails.isCanon()) {
            // 캐논 계정 → 조회 페이지로
            return "redirect:/canon/view";
        } else if (userDetails.isMynet()) {
            // 마이넷 계정 → 조회 페이지로
            return "redirect:/mynet/view";
        } else {
            // 하위회사 계정 → 입력 페이지로
            return "redirect:/subsidiary/input";
        }
    }

    /**
     * 접근 권한 없음 페이지
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}