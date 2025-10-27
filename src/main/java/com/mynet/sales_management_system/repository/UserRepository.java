// UserRepository.java - 사용자 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 데이터 접근 레포지토리
 * - 로그인 인증용 사용자 조회
 * - 회사별 사용자 관리
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자 조회 (로그인용)
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 회사별 사용자 목록 조회
     */
    List<User> findByCompanyId(Long companyId);
    
    /**
     * 캐논 계정 조회
     */
    Optional<User> findByIsCanonTrue();
}