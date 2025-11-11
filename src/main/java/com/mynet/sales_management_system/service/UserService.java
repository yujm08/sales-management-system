package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.User;
import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.repository.UserRepository;
import com.mynet.sales_management_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String username, String password, Long companyId, Role role, boolean isCanon) {
        // 중복 사용자명 체크
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다: " + companyId));

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username)
                .password(encodedPassword)
                .company(company)
                .role(role)
                .isCanon(isCanon)
                .build();

        log.info("사용자 생성: {}, 회사: {}", username, company.getName());
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
        log.info("사용자 삭제: ID={}", userId);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 새 비밀번호 암호화 및 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("비밀번호 변경 완료: 사용자={}", username);
    }
}