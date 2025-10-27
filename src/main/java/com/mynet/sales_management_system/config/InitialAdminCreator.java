package com.mynet.sales_management_system.config;

import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.service.UserService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialAdminCreator implements CommandLineRunner {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.create:false}")
    private boolean shouldCreateAdmin;

    private final UserService userService;
    private final CompanyRepository companyRepository;

    @Override
    public void run(String... args) {
        // 관리자가 없고, 환경변수로 생성 설정이 되어있으면 생성
        if (shouldCreateAdmin && adminPassword != null && !adminPassword.isEmpty()) {
            boolean hasAdmin = userService.getAllUsers().stream()
                    .anyMatch(u -> u.getRole() == Role.ADMIN);

            if (!hasAdmin) {
                Company mynet = companyRepository.findByIsMynetTrue()
                        .orElseThrow(() -> new RuntimeException("마이넷 회사가 없습니다."));

                userService.createUser(adminUsername, adminPassword, mynet.getId(), Role.ADMIN, false);
                log.info("초기 관리자 계정 생성됨: {}", adminUsername);
            }
        }
    }
}