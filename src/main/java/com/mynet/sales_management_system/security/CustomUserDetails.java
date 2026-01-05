// CustomUserDetails.java - 사용자 정보를 담는 클래스
package com.mynet.sales_management_system.security;

import com.mynet.sales_management_system.entity.Role;
import com.mynet.sales_management_system.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails 구현체
 * - 데이터베이스의 User 엔티티를 Security 컨텍스트에서 사용할 수 있도록 변환
 * - 권한을 마이넷/하위회사/캐논으로 구분하여 설정
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 관리 권한
        if (user.getRole() == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // 업무 영역 권한 (단일)
        if (user.getIsCanon()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CANON"));
        } else if (user.getCompany().getIsMynet()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MYNET"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUBSIDIARY"));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // 계정 관련 설정들 (모두 활성화)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 추가 사용자 정보 접근을 위한 메서드
    public User getUser() {
        return user;
    }

    public Long getCompanyId() {
        return user.getCompany().getId();
    }

    public String getCompanyName() {
        return user.getCompany().getName();
    }

    public boolean isMynet() {
        return user.getCompany().getIsMynet();
    }

    public boolean isCanon() {
        return user.getIsCanon();
    }

}