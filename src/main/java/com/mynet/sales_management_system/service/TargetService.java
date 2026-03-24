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

import java.util.Comparator;
import java.util.List;
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
     * 전체 목표를 6개 하위 회사에 균등 분배
     * 나머지는 원이스토리(쿠팡)에 추가
     */
    @Transactional
    public void distributeTarget(Long productId, int year, int month, int totalQuantity, String createdBy) {

        // 마이넷(is_mynet=true), 마이넷(GX판매), 캐논 제외 → 6개 회사
        List<Company> subsidiaries = companyRepository.findByIsMynetFalse()
                .stream()
                .filter(c -> !"캐논".equals(c.getName()) && !c.getName().contains("마이넷"))
                .sorted(Comparator.comparing(Company::getId))
                .collect(Collectors.toList());

        int count = subsidiaries.size();
        if (count == 0) {
            log.warn("분배할 하위 회사가 없습니다.");
            return;
        }

        int base = totalQuantity / count;
        int remainder = totalQuantity % count;

        // 나머지 수량을 받을 회사: 원이스토리(쿠팡)
        Company remainderCompany = subsidiaries.stream()
                .filter(c -> c.getName().contains("원이스토리") || c.getName().contains("쿠팡"))
                .findFirst()
                .orElse(subsidiaries.get(subsidiaries.size() - 1)); // 없으면 마지막 회사에

        for (Company company : subsidiaries) {
            int qty = base + (company.getId().equals(remainderCompany.getId()) ? remainder : 0);
            saveTarget(company.getId(), productId, year, month, qty, createdBy);
        }

        log.info("목표 분배 완료: 제품ID={}, 기간={}-{}, 총수량={}, 회사수={}, 기본배분={}, 나머지={}",
                productId, year, month, totalQuantity, count, base, remainder);
    }
}