// CompanyRepository.java - 회사 데이터 접근
package com.mynet.sales_management_system.repository;

import com.mynet.sales_management_system.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회사 데이터 접근 레포지토리
 * - 하위 회사 목록 관리
 * - 마이넷 회사 조회
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * 마이넷 회사 조회
     */
    Optional<Company> findByIsMynetTrue();
    
    /**
     * 하위 회사 목록 조회 (마이넷 제외)
     */
    List<Company> findByIsMynetFalse();
    
    /**
     * 회사명으로 조회
     */
    Optional<Company> findByName(String name);
}