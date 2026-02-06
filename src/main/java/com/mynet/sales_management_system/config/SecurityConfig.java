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
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 보안 설정 클래스
 * - 세션 기반 인증 사용
 * - 권한별 접근 제어 설정
 * - 마이넷/하위회사/캐논 계정별 차등 권한 적용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

        /**
         * 비밀번호 암호화를 위한 BCrypt 인코더
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
                // return NoOpPasswordEncoder.getInstance(); // 임시로 평문 비밀번호 허용
        }

        // 엄격한 HTTP Firewall
        @Bean
        public HttpFirewall httpFirewall() {
                StrictHttpFirewall firewall = new StrictHttpFirewall();

                // SQL 인젝션에 자주 사용되는 문자 차단
                firewall.setAllowSemicolon(false); // ; 차단 (쿼리 구분자)
                firewall.setAllowUrlEncodedSlash(false); // %2F 차단
                firewall.setAllowBackSlash(false); // \ 차단
                firewall.setAllowUrlEncodedPercent(false); // %25 차단
                firewall.setAllowUrlEncodedPeriod(false); // %2E 차단
                firewall.setAllowUrlEncodedDoubleSlash(false); // %2F%2F 차단

                return firewall;
        }

        /**
         * HTTP 보안 필터 체인 설정
         * - 페이지별 접근 권한 제어
         * - 로그인/로그아웃 설정
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http

                                .headers(headers -> headers
                                                // XSS 보호
                                                .xssProtection(xss -> xss
                                                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                // Clickjacking 방어
                                                .frameOptions(frame -> frame.deny())
                                                // MIME 타입 스니핑 방지
                                                .contentTypeOptions(contentType -> {
                                                })
                                                // Content Security Policy
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';")))

                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                                // 403 에러 처리 핸들러 추가
                                .exceptionHandling(exception -> exception
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        log.warn("403 Access Denied: {} - User: {}",
                                                                        request.getRequestURI(),
                                                                        request.getUserPrincipal() != null
                                                                                        ? request.getUserPrincipal()
                                                                                                        .getName()
                                                                                        : "anonymous");

                                                        // 세션 무효화
                                                        jakarta.servlet.http.HttpSession session = request
                                                                        .getSession(false);
                                                        if (session != null) {
                                                                session.invalidate();
                                                        }

                                                        // 캐시 방지 헤더 설정
                                                        response.setHeader("Cache-Control",
                                                                        "no-cache, no-store, must-revalidate");
                                                        response.setHeader("Pragma", "no-cache");
                                                        response.setHeader("Expires", "0");

                                                        // 403 에러 페이지로 리다이렉트
                                                        response.sendRedirect("/error-403");
                                                }))
                                .authorizeHttpRequests(authz -> authz
                                                // 정적 리소스는 모든 사용자 접근 허용
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // /login은 비로그인 사용자만 (이미 로그인된 경우 접근 차단)
                                                .requestMatchers("/login", "/error").anonymous()

                                                // 회원가입, 홈, 에러 페이지는 모든 사용자 접근 허용
                                                .requestMatchers("/signup", "/", "/error").permitAll()

                                                // 하위회사 전용 페이지 (입력, 통계, 월별매출집계)
                                                .requestMatchers("/subsidiary/**").hasRole("SUBSIDIARY")

                                                // 마이넷 전용 페이지 (조회, 비교, 제품분류)
                                                .requestMatchers("/mynet/**").hasAnyRole("MYNET", "ADMIN")

                                                // 관리자 페이지 (사용자 관리 등)
                                                .requestMatchers("/admin/products")
                                                .hasAnyRole("MYNET", "CANON", "ADMIN")
                                                .requestMatchers("/admin/**").hasAnyRole("MYNET", "ADMIN")

                                                // 캐논 전용 페이지 (조회, 비교)
                                                .requestMatchers("/canon/**").hasAnyRole("CANON", "ADMIN")

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