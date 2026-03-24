package com.mynet.sales_management_system.config;

import com.mynet.sales_management_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * 모든 컨트롤러의 Model에 자동으로 추가되는 속성
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            boolean isCanon = userDetails.isCanon();
            boolean isMynet = userDetails.isMynet();

            // 전역으로 사용 가능하도록 Model에 추가
            model.addAttribute("isCanon", isCanon);
            model.addAttribute("isMynet", isMynet);
            model.addAttribute("currentUser", userDetails.getCompanyName());

            // 권한 정보
            boolean hasAdminAccess = !isCanon; // Canon이 아니면 관리 권한
            model.addAttribute("hasAdminAccess", hasAdminAccess);
        }
    }
}