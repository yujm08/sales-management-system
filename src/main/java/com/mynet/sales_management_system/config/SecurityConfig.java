// SecurityConfig.java - Spring Security 설정
package com.mynet.sales_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 보안 설정 클래스
 * - 세션 기반 인증 사용
 * - 권한별 접근 제어 설정
 * - 마이넷/하위회사/캐논 계정별 차등 권한 적용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        /**
         * 비밀번호 암호화를 위한 BCrypt 인코더
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
                // return NoOpPasswordEncoder.getInstance(); // 임시로 평문 비밀번호 허용
        }

        /**
         * HTTP 보안 필터 체인 설정
         * - 페이지별 접근 권한 제어
         * - 로그인/로그아웃 설정
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                // 정적 리소스는 모든 사용자 접근 허용
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // 로그인, 회원가입, 에러 페이지는 모든 사용자 접근 허용
                                                .requestMatchers("/login", "/signup", "/", "/error").permitAll()
                                                // 하위회사 전용 페이지 (입력, 통계, 월별매출집계)
                                                .requestMatchers("/subsidiary/**").hasRole("SUBSIDIARY")
                                                // 마이넷 전용 페이지 (조회, 비교, 제품분류)
                                                .requestMatchers("/mynet/**").hasAnyRole("MYNET", "CANON")
                                                // 관리자 페이지 (사용자 관리 등) - ADMIN 권한 필요
                                                .requestMatchers("/admin/products").hasAnyRole("MYNET", "CANON")
                                                .requestMatchers("/admin/**").hasRole("MYNET")
                                                // 나머지 모든 요청은 인증 필요
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard", true) // 로그인 성공 시 대시보드로 리다이렉트
                                                .failureUrl("/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .sessionManagement(session -> session
                                                .maximumSessions(10) // 동시 접속 허용 (여러 명이 동시 사용 가능)
                                                .maxSessionsPreventsLogin(false));

                return http.build();
        }
}