// SalesService.java - 실적 관리 서비스  
package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;
import com.mynet.sales_management_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SalesService {

    private final DailySalesRepository dailySalesRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    /**
     * 특정 회사의 특정 날짜 실적 조회
     */
    public List<DailySales> getDailySalesByCompanyAndDate(Long companyId, LocalDate salesDate) {
        return dailySalesRepository
                .findByCompanyIdAndSalesDateOrderByProduct_CategoryAscProduct_ProductCodeAsc(companyId, salesDate);
    }

    /**
     * 전체 회사의 특정 날짜 실적 조회 (마이넷용)
     */
    public List<DailySales> getAllDailySalesByDate(LocalDate salesDate) {
        return dailySalesRepository.findBySalesDateOrderByCompanyAndProduct(salesDate);
    }

    /**
     * 특정 회사의 월별 실적 조회
     */
    public List<DailySales> getMonthlySalesByCompany(Long companyId, int year, int month) {
        return dailySalesRepository.findByCompanyIdAndYearAndMonth(companyId, year, month);
    }

    /**
     * 실적 데이터 입력/수정
     */
    @Transactional
    public DailySales saveDailySales(Long companyId, Long productId, LocalDate salesDate,
            Integer quantity, String modifiedBy) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다: " + companyId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다: " + productId));

        // 기존 데이터 확인
        Optional<DailySales> existingOpt = dailySalesRepository
                .findByCompanyIdAndProductIdAndSalesDate(companyId, productId, salesDate);

        DailySales dailySales;
        if (existingOpt.isPresent()) {
            // 기존 데이터 업데이트
            dailySales = existingOpt.get();
            dailySales.setQuantity(quantity);
            dailySales.setModifiedBy(modifiedBy);
        } else {
            // 새 데이터 생성
            dailySales = DailySales.builder()
                    .company(company)
                    .product(product)
                    .salesDate(salesDate)
                    .quantity(quantity)
                    .modifiedBy(modifiedBy)
                    .build();
        }

        dailySales = dailySalesRepository.save(dailySales);

        log.info("실적 데이터 저장: 회사={}, 제품={}, 날짜={}, 수량={}",
                company.getName(), product.getProductCode(), salesDate, quantity);

        return dailySales;
    }

    /**
     * 특정 제품의 기간별 실적 조회 (기간별 비교용)
     */
    public List<DailySales> getSalesByProductAndDateRange(Long productId, LocalDate startDate, LocalDate endDate) {
        return dailySalesRepository.findByProductIdAndDateRange(productId, startDate, endDate);
    }

    /**
     * 전체 제품의 기간별 실적 조회
     */
    public List<DailySales> getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        return dailySalesRepository.findByDateRangeOrderByDateAndProduct(startDate, endDate);
    }

    /**
     * 실적 데이터 삭제 (마이넷 전용)
     */
    @Transactional
    public void deleteDailySales(Long companyId, Long productId, LocalDate salesDate) {
        Optional<DailySales> salesOpt = dailySalesRepository
                .findByCompanyIdAndProductIdAndSalesDate(companyId, productId, salesDate);

        if (salesOpt.isPresent()) {
            dailySalesRepository.delete(salesOpt.get());
            log.info("실적 데이터 삭제: 회사ID={}, 제품ID={}, 날짜={}", companyId, productId, salesDate);
        }
    }

    /**
     * 당월 데이터 여부 확인 (하위회사 수정 권한 체크용)
     */
    public boolean isCurrentMonth(LocalDate salesDate) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth salesMonth = YearMonth.from(salesDate);
        return currentMonth.equals(salesMonth);
    }
}