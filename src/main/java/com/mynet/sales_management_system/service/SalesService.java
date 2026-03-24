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
                                .findByCompanyIdAndSalesDateOrderByProduct_CategoryAscProduct_ProductCodeAsc(companyId,
                                                salesDate);
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
}