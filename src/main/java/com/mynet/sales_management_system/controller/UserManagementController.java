package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.User;
import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.service.UserService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final UserService userService;
    private final CompanyRepository companyRepository;

    /**
     * 사용자 관리 페이지 (마이넷 관리자만 접근 가능)
     */
    @GetMapping("/users")
    public String userManagementPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // 마이넷 관리자가 아니면 접근 불가
        if (!userDetails.isMynet() || !userDetails.getUser().getRole().equals(Role.ADMIN)) {
            return "redirect:/access-denied";
        }

        List<User> users = userService.getAllUsers();
        List<Company> companies = companyRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("companies", companies);
        model.addAttribute("roles", Role.values());

        return "admin/user-management"; // fragment 없이 직접 템플릿 반환
    }

    /**
     * 사용자 생성
     */
    @PostMapping("/users/create")
    @ResponseBody
    public String createUser(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Long companyId,
            @RequestParam Role role,
            @RequestParam(defaultValue = "false") boolean isCanon) {
        try {
            // 권한 체크
            if (!userDetails.isMynet() || !userDetails.getUser().getRole().equals(Role.ADMIN)) {
                return "error:권한이 없습니다.";
            }

            userService.createUser(username, password, companyId, role, isCanon);
            log.info("사용자 생성: 관리자={}, 새사용자={}", userDetails.getUsername(), username);

            return "success";
        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            return "error:" + e.getMessage();
        }
    }

    /**
     * 사용자 삭제
     */
    @PostMapping("/users/delete")
    @ResponseBody
    public String deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long userId) {
        try {
            // 권한 체크
            if (!userDetails.isMynet() || !userDetails.getUser().getRole().equals(Role.ADMIN)) {
                return "error:권한이 없습니다.";
            }

            // 자기 자신은 삭제 불가
            if (userDetails.getUser().getId().equals(userId)) {
                return "error:자기 자신은 삭제할 수 없습니다.";
            }

            userService.deleteUser(userId);
            log.info("사용자 삭제: 관리자={}, 삭제된사용자ID={}", userDetails.getUsername(), userId);

            return "success";
        } catch (Exception e) {
            log.error("사용자 삭제 실패", e);
            return "error:" + e.getMessage();
        }
    }
}