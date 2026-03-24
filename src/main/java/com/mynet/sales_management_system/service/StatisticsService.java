// StatisticsService.java - 통계 및 집계 서비스
package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.YearlyComparisonDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 통계 및 집계 서비스
 * - 매출 금액 계산 (수량 × 공급가)
 * - 이익 계산 (공급가 - 원가)
 * - 달성률 계산
 * - 전일/전월 비교 데이터 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {

        private final DailySalesRepository dailySalesRepository;
        private final ProductService productService;
        private final ProductRepository productRepository;

        /**
         * 년도별 비교 데이터 조회 (제품별 × 년도별)
         * 하위회사 전체의 제품별 연간 합계
         */
        public YearlyComparisonResponse getYearlyComparisonData(int startYear, int endYear) {
                List<Product> activeProducts = productRepository
                                .findByIsActiveTrueOrderByCategoryAscProductCodeAsc();

                // 카테고리별로 그룹화
                Map<String, List<Product>> productsByCategory = activeProducts.stream()
                                .collect(Collectors.groupingBy(
                                                Product::getCategory,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                List<YearlyComparisonDTO.CategoryData> categoryDataList = new ArrayList<>();

                BigDecimal grandYear1 = BigDecimal.ZERO;
                BigDecimal grandYear2 = BigDecimal.ZERO;
                BigDecimal grandYear3 = BigDecimal.ZERO;
                int grandQty1 = 0, grandQty2 = 0, grandQty3 = 0;

                // 각 카테고리별로 처리
                for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
                        String category = entry.getKey();
                        List<Product> categoryProducts = entry.getValue();

                        List<YearlyComparisonDTO.ProductYearlyData> productDataList = new ArrayList<>();

                        BigDecimal catYear1 = BigDecimal.ZERO;
                        BigDecimal catYear2 = BigDecimal.ZERO;
                        BigDecimal catYear3 = BigDecimal.ZERO;
                        int catQty1 = 0, catQty2 = 0, catQty3 = 0;

                        for (Product product : categoryProducts) {
                                // 각 년도별 연간 합계 계산
                                YearlyProductTotal year1 = calculateProductYearlyTotal(product.getId(), startYear);
                                YearlyProductTotal year2 = calculateProductYearlyTotal(product.getId(), startYear + 1);
                                YearlyProductTotal year3 = calculateProductYearlyTotal(product.getId(), endYear);

                                BigDecimal growthRate = calculateGrowthRate(
                                                BigDecimal.valueOf(year2.getTotalQuantity()),
                                                BigDecimal.valueOf(year3.getTotalQuantity()));

                                productDataList.add(YearlyComparisonDTO.ProductYearlyData.builder()
                                                .productId(product.getId())
                                                .productCode(product.getProductCode())
                                                .productName(product.getProductName())
                                                .category(category)
                                                .year1Amount(year1.getTotalAmount())
                                                .year2Amount(year2.getTotalAmount())
                                                .year3Amount(year3.getTotalAmount())
                                                .year1Quantity(year1.getTotalQuantity())
                                                .year2Quantity(year2.getTotalQuantity())
                                                .year3Quantity(year3.getTotalQuantity())
                                                .growthRate(growthRate)
                                                .build());

                                catYear1 = catYear1.add(year1.getTotalAmount());
                                catYear2 = catYear2.add(year2.getTotalAmount());
                                catYear3 = catYear3.add(year3.getTotalAmount());

                                catQty1 += year1.getTotalQuantity();
                                catQty2 += year2.getTotalQuantity();
                                catQty3 += year3.getTotalQuantity();
                        }

                        // 카테고리별 합계
                        YearlyComparisonDTO.YearlyTotal categoryTotal = YearlyComparisonDTO.YearlyTotal.builder()
                                        .year1Amount(catYear1)
                                        .year2Amount(catYear2)
                                        .year3Amount(catYear3)
                                        .year1Quantity(catQty1)
                                        .year2Quantity(catQty2)
                                        .year3Quantity(catQty3)
                                        .growthRate(calculateGrowthRate(
                                                        BigDecimal.valueOf(catQty2), BigDecimal.valueOf(catQty3)))
                                        .build();

                        categoryDataList.add(YearlyComparisonDTO.CategoryData.builder()
                                        .category(category)
                                        .products(productDataList)
                                        .categoryTotal(categoryTotal)
                                        .build());

                        grandYear1 = grandYear1.add(catYear1);
                        grandYear2 = grandYear2.add(catYear2);
                        grandYear3 = grandYear3.add(catYear3);
                        grandQty1 += catQty1;
                        grandQty2 += catQty2;
                        grandQty3 += catQty3;
                }

                // 전체 합계
                YearlyComparisonDTO.GrandTotal grandTotal = YearlyComparisonDTO.GrandTotal.builder()
                                .year1Amount(grandYear1)
                                .year2Amount(grandYear2)
                                .year3Amount(grandYear3)
                                .year1Quantity(grandQty1)
                                .year2Quantity(grandQty2)
                                .year3Quantity(grandQty3)
                                .growthRate(calculateGrowthRate(
                                                BigDecimal.valueOf(grandQty2), BigDecimal.valueOf(grandQty3)))
                                .build();

                return YearlyComparisonResponse.builder()
                                .categories(categoryDataList)
                                .grandTotal(grandTotal)
                                .startYear(startYear)
                                .endYear(endYear)
                                .build();
        }

        /**
         * 특정 제품의 특정 년도 연간 합계 금액 및 수량 계산
         */
        private YearlyProductTotal calculateProductYearlyTotal(Long productId, int year) {
                BigDecimal totalAmount = BigDecimal.ZERO;
                int totalQuantity = 0;

                for (int month = 1; month <= 12; month++) {
                        List<DailySales> salesList = dailySalesRepository
                                        .findByProductIdAndYearAndMonthForComparison(productId, year, month);

                        for (DailySales sales : salesList) {
                                totalQuantity += sales.getQuantity();

                                LocalDateTime salesDateTime = sales.getSalesDate().atTime(23, 59, 59);
                                Optional<ProductPriceHistory> priceHistory = productService
                                                .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime);

                                if (priceHistory.isPresent()) {
                                        BigDecimal amount = priceHistory.get().getSupplyPrice()
                                                        .multiply(BigDecimal.valueOf(sales.getQuantity()));
                                        totalAmount = totalAmount.add(amount);
                                }
                        }
                }

                return YearlyProductTotal.builder()
                                .totalAmount(totalAmount)
                                .totalQuantity(totalQuantity)
                                .build();
        }

        // 내부 클래스 추가 (StatisticsService 맨 아래)
        @Data
        @Builder
        private static class YearlyProductTotal {
                private BigDecimal totalAmount;
                private Integer totalQuantity;
        }

        /**
         * 증감률 계산 (전년 대비 올해)
         */
        private BigDecimal calculateGrowthRate(BigDecimal previousAmount, BigDecimal currentAmount) {
                if (previousAmount.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                }

                return currentAmount.subtract(previousAmount)
                                .divide(previousAmount, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
        }

        // 응답 DTO
        @Data
        @Builder
        public static class YearlyComparisonResponse {
                private List<YearlyComparisonDTO.CategoryData> categories;
                private YearlyComparisonDTO.GrandTotal grandTotal;
                private int startYear;
                private int endYear;
        }
}