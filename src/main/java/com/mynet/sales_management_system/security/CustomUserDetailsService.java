// CustomUserDetailsService.java - 사용자 인증 서비스
package com.mynet.sales_management_system.security;

import com.mynet.sales_management_system.entity.User;
import com.mynet.sales_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 사용자 인증 서비스
 * - 로그인 시 사용자명으로 데이터베이스에서 사용자 정보 조회
 * - User 엔티티를 CustomUserDetails로 변환하여 반환
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        return new CustomUserDetails(user);
    }
}