// TargetService.java - 목표 관리 서비스
package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.entity.Target;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.repository.TargetRepository;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 목표 관리 서비스
 * - 마이넷에서만 목표 설정 가능
 * - 전체 목표와 개별 회사 목표 관리
 * - 목표 합계 검증 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TargetService {
    
    private final TargetRepository targetRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    
    /**
     * 특정 회사의 특정 월 목표 조회
     */
    public List<Target> getTargetsByCompanyAndMonth(Long companyId, int year, int month) {
        return targetRepository.findByCompanyIdAndTargetYearAndTargetMonth(companyId, year, month);
    }
    
    /**
     * 전체 목표 조회
     */
    public List<Target> getGlobalTargets(int year, int month) {
        return targetRepository.findGlobalTargetsByYearAndMonth(year, month);
    }
    
    /**
     * 목표 설정/수정
     */
    @Transactional
    public Target saveTarget(Long companyId, Long productId, int year, int month, 
                           int targetQuantity, String createdBy) {
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다: " + productId));
        
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다: " + companyId));
        }
        
        // 기존 목표 확인
        Optional<Target> existingOpt;
        if (companyId != null) {
            existingOpt = targetRepository
                .findByCompanyIdAndProductIdAndTargetYearAndTargetMonth(companyId, productId, year, month);
        } else {
            existingOpt = targetRepository
                .findGlobalTargetByProductAndYearAndMonth(productId, year, month);
        }
        
        Target target;
        if (existingOpt.isPresent()) {
            // 기존 목표 업데이트
            target = existingOpt.get();
            target.setTargetQuantity(targetQuantity);
        } else {
            // 새 목표 생성
            target = Target.builder()
                .company(company)
                .product(product)
                .targetYear(year)
                .targetMonth(month)
                .targetQuantity(targetQuantity)
                .createdBy(createdBy)
                .build();
        }
        
        target = targetRepository.save(target);
        
        String targetType = (companyId == null) ? "전체" : company.getName();
        log.info("목표 설정: 대상={}, 제품={}, 기간={}-{}, 목표={}", 
                targetType, product.getProductCode(), year, month, targetQuantity);
        
        return target;
    }
    
    /**
     * 개별 회사 목표 합계가 전체 목표와 일치하는지 검증
     */
    public boolean validateTargetSum(Long productId, int year, int month) {
        // 전체 목표 조회
        Optional<Target> globalTargetOpt = targetRepository
            .findGlobalTargetByProductAndYearAndMonth(productId, year, month);
        
        if (globalTargetOpt.isEmpty()) {
            return true; // 전체 목표가 없으면 검증 통과
        }
        
        int globalTarget = globalTargetOpt.get().getTargetQuantity();
        
        // 개별 회사 목표 합계 계산
        List<Object[]> companySums = targetRepository.findCompanyTargetSumsByYearAndMonth(year, month);
        
        int companyTargetSum = companySums.stream()
            .filter(row -> row[0].equals(productId))
            .mapToInt(row -> ((Number) row[1]).intValue())
            .sum();
        
        return globalTarget == companyTargetSum;
    }
    
    /**
     * 목표 삭제
     */
    @Transactional
    public void deleteTarget(Long companyId, Long productId, int year, int month) {
        Optional<Target> targetOpt;
        if (companyId != null) {
            targetOpt = targetRepository
                .findByCompanyIdAndProductIdAndTargetYearAndTargetMonth(companyId, productId, year, month);
        } else {
            targetOpt = targetRepository
                .findGlobalTargetByProductAndYearAndMonth(productId, year, month);
        }
        
        if (targetOpt.isPresent()) {
            targetRepository.delete(targetOpt.get());
            String targetType = (companyId == null) ? "전체" : "개별회사";
            log.info("목표 삭제: 대상={}, 제품ID={}, 기간={}-{}", targetType, productId, year, month);
        }
    }
}