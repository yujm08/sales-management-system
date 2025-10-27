// StatisticsService.java - 통계 및 집계 서비스
package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.YearlyComparisonDTO;
import com.mynet.sales_management_system.dto.YearlyData;
import com.mynet.sales_management_system.dto.YearlyProductData;
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
import java.time.LocalDate;
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
     * 실적 데이터에 가격 정보를 포함한 통계 계산
     */
    public SalesStatistics calculateSalesStatistics(List<DailySales> salesList) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (DailySales sales : salesList) {
            // 해당 날짜의 가격 정보 조회
            LocalDateTime salesDateTime = sales.getSalesDate().atStartOfDay();
            ProductPriceHistory priceHistory = productService
                    .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime)
                    .orElse(null);

            if (priceHistory != null) {
                BigDecimal quantity = BigDecimal.valueOf(sales.getQuantity());
                BigDecimal revenue = priceHistory.getSupplyPrice().multiply(quantity);
                BigDecimal cost = priceHistory.getCostPrice().multiply(quantity);
                BigDecimal profit = revenue.subtract(cost);

                totalRevenue = totalRevenue.add(revenue);
                totalProfit = totalProfit.add(profit);
            }

            totalQuantity += sales.getQuantity();
        }

        return SalesStatistics.builder()
                .totalQuantity(totalQuantity)
                .totalRevenue(totalRevenue)
                .totalProfit(totalProfit)
                .build();
    }

    /**
     * 달성률 계산
     */
    public BigDecimal calculateAchievementRate(int actualQuantity, int targetQuantity) {
        if (targetQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(actualQuantity)
                .divide(BigDecimal.valueOf(targetQuantity), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 전일 대비 비교 데이터 생성
     */
    public ComparisonData generateDailyComparison(Long companyId, LocalDate currentDate) {
        LocalDate previousDate = currentDate.minusDays(1);

        List<DailySales> currentSales = dailySalesRepository
                .findByCompanyIdAndSalesDateOrderByProduct_CategoryAscProduct_ProductCodeAsc(companyId, currentDate);
        List<DailySales> previousSales = dailySalesRepository
                .findByCompanyIdAndSalesDateOrderByProduct_CategoryAscProduct_ProductCodeAsc(companyId, previousDate);

        SalesStatistics currentStats = calculateSalesStatistics(currentSales);
        SalesStatistics previousStats = calculateSalesStatistics(previousSales);

        return ComparisonData.builder()
                .currentProfit(currentStats.getTotalProfit())
                .previousProfit(previousStats.getTotalProfit())
                .profitChange(currentStats.getTotalProfit().subtract(previousStats.getTotalProfit()))
                .isIncrease(currentStats.getTotalProfit().compareTo(previousStats.getTotalProfit()) > 0)
                .build();
    }

    /**
     * 전월 대비 비교 데이터 생성
     */
    public ComparisonData generateMonthlyComparison(Long companyId, int currentYear, int currentMonth) {
        int previousYear = currentYear;
        int previousMonth = currentMonth - 1;

        if (previousMonth == 0) {
            previousYear = currentYear - 1;
            previousMonth = 12;
        }

        List<DailySales> currentSales = dailySalesRepository
                .findByCompanyIdAndYearAndMonth(companyId, currentYear, currentMonth);
        List<DailySales> previousSales = dailySalesRepository
                .findByCompanyIdAndYearAndMonth(companyId, previousYear, previousMonth);

        SalesStatistics currentStats = calculateSalesStatistics(currentSales);
        SalesStatistics previousStats = calculateSalesStatistics(previousSales);

        return ComparisonData.builder()
                .currentProfit(currentStats.getTotalProfit())
                .previousProfit(previousStats.getTotalProfit())
                .profitChange(currentStats.getTotalProfit().subtract(previousStats.getTotalProfit()))
                .isIncrease(currentStats.getTotalProfit().compareTo(previousStats.getTotalProfit()) > 0)
                .build();
    }

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

        // 각 카테고리별로 처리
        for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
            String category = entry.getKey();
            List<Product> categoryProducts = entry.getValue();

            List<YearlyComparisonDTO.ProductYearlyData> productDataList = new ArrayList<>();

            BigDecimal catYear1 = BigDecimal.ZERO;
            BigDecimal catYear2 = BigDecimal.ZERO;
            BigDecimal catYear3 = BigDecimal.ZERO;

            for (Product product : categoryProducts) {
                // 각 년도별 연간 합계 계산
                BigDecimal year1Amount = calculateProductYearlyAmount(product.getId(), startYear);
                BigDecimal year2Amount = calculateProductYearlyAmount(product.getId(), startYear + 1);
                BigDecimal year3Amount = calculateProductYearlyAmount(product.getId(), endYear);

                // 증감률 계산 (전년 대비 올해)
                BigDecimal growthRate = calculateGrowthRate(year2Amount, year3Amount);

                productDataList.add(YearlyComparisonDTO.ProductYearlyData.builder()
                        .productId(product.getId())
                        .productCode(product.getProductCode())
                        .productName(product.getProductName())
                        .category(category)
                        .year1Amount(year1Amount)
                        .year2Amount(year2Amount)
                        .year3Amount(year3Amount)
                        .growthRate(growthRate)
                        .build());

                catYear1 = catYear1.add(year1Amount);
                catYear2 = catYear2.add(year2Amount);
                catYear3 = catYear3.add(year3Amount);
            }

            // 카테고리별 합계
            YearlyComparisonDTO.YearlyTotal categoryTotal = YearlyComparisonDTO.YearlyTotal.builder()
                    .year1Amount(catYear1)
                    .year2Amount(catYear2)
                    .year3Amount(catYear3)
                    .growthRate(calculateGrowthRate(catYear2, catYear3))
                    .build();

            categoryDataList.add(YearlyComparisonDTO.CategoryData.builder()
                    .category(category)
                    .products(productDataList)
                    .categoryTotal(categoryTotal)
                    .build());

            grandYear1 = grandYear1.add(catYear1);
            grandYear2 = grandYear2.add(catYear2);
            grandYear3 = grandYear3.add(catYear3);
        }

        // 전체 합계
        YearlyComparisonDTO.GrandTotal grandTotal = YearlyComparisonDTO.GrandTotal.builder()
                .year1Amount(grandYear1)
                .year2Amount(grandYear2)
                .year3Amount(grandYear3)
                .growthRate(calculateGrowthRate(grandYear2, grandYear3))
                .build();

        return YearlyComparisonResponse.builder()
                .categories(categoryDataList)
                .grandTotal(grandTotal)
                .startYear(startYear)
                .endYear(endYear)
                .build();
    }

    /**
     * 특정 제품의 특정 년도 연간 합계 금액 계산
     */
    private BigDecimal calculateProductYearlyAmount(Long productId, int year) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            List<DailySales> salesList = dailySalesRepository
                    .findByProductIdAndYearAndMonthForComparison(productId, year, month);

            for (DailySales sales : salesList) {
                LocalDateTime salesDateTime = sales.getSalesDate().atStartOfDay();
                Optional<ProductPriceHistory> priceHistory = productService
                        .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime);

                if (priceHistory.isPresent()) {
                    BigDecimal amount = priceHistory.get().getSupplyPrice()
                            .multiply(BigDecimal.valueOf(sales.getQuantity()));
                    totalAmount = totalAmount.add(amount);
                }
            }
        }

        return totalAmount;
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

    /**
     * 특정 년월의 전체 하위회사 데이터 집계
     */
    private YearlyData calculateYearlyMonthData(int year, int month) {
        List<DailySales> salesList = dailySalesRepository
                .findAllByYearAndMonth(year, month);

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DailySales sales : salesList) {
            totalQuantity += sales.getQuantity();

            LocalDateTime salesDateTime = sales.getSalesDate().atStartOfDay();
            Optional<ProductPriceHistory> priceHistory = productService
                    .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime);

            if (priceHistory.isPresent()) {
                BigDecimal quantity = BigDecimal.valueOf(sales.getQuantity());
                BigDecimal amount = priceHistory.get().getSupplyPrice().multiply(quantity);
                totalAmount = totalAmount.add(amount);
            }
        }

        return YearlyData.builder()
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 특정 제품의 특정 년월의 전체 하위회사 데이터 집계
     */
    private YearlyProductData calculateYearlyProductMonthData(Long productId, int year, int month) {
        List<DailySales> salesList = dailySalesRepository
                .findByProductIdAndYearAndMonthForComparison(productId, year, month);

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DailySales sales : salesList) {
            totalQuantity += sales.getQuantity();

            LocalDateTime salesDateTime = sales.getSalesDate().atStartOfDay();
            Optional<ProductPriceHistory> priceHistory = productService
                    .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime);

            if (priceHistory.isPresent()) {
                BigDecimal quantity = BigDecimal.valueOf(sales.getQuantity());
                BigDecimal amount = priceHistory.get().getSupplyPrice().multiply(quantity);
                totalAmount = totalAmount.add(amount);
            }
        }

        return YearlyProductData.builder()
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

}