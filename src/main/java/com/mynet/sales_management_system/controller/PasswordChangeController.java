package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 비밀번호 변경 컨트롤러
 * - 모든 사용자 (마이넷/캐논) 공통 사용
 */
@Controller
@RequestMapping("/change-password")
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeController {

    private final UserService userService;

    /**
     * 비밀번호 변경 페이지
     */
    @GetMapping
    public String changePasswordPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("companyName", userDetails.getCompanyName());

        log.info("비밀번호 변경 페이지 접근: 사용자={}", userDetails.getUsername());
        return "/changePassword";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            // 새 비밀번호 확인
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/change-password";
            }

            // 비밀번호 길이 체크
            if (newPassword.length() < 4) {
                redirectAttributes.addFlashAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
                return "redirect:/change-password";
            }

            // 비밀번호 변경
            userService.changePassword(userDetails.getUsername(), currentPassword, newPassword);

            log.info("비밀번호 변경 성공: 사용자={}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "비밀번호가 성공적으로 변경되었습니다.");

            // 사용자 역할에 따라 리다이렉트
            if (userDetails.isCanon()) {
                return "redirect:/canon/view";
            } else if (userDetails.isMynet()) {
                return "redirect:/mynet/view";
            } else {
                return "redirect:/subsidiary/input";
            }

        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: 사용자={}, 사유={}",
                    userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/change-password";
        } catch (Exception e) {
            log.error("비밀번호 변경 중 오류 발생: 사용자={}",
                    userDetails.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "비밀번호 변경 중 오류가 발생했습니다.");
            return "redirect:/change-password";
        }
    }
}