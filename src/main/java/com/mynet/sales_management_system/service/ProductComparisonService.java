package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.ProductComparisonDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductComparisonService {

    private final DailySalesRepository dailySalesRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * 제품별 비교 데이터 조회
     * 
     * @param productId null이면 전체 제품 합계
     */
    public ProductComparisonDTO getProductComparison(Long productId) {
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = List.of(currentYear - 2, currentYear - 1, currentYear);

        ProductComparisonDTO.ProductInfo productInfo;

        if (productId == null) {
            productInfo = ProductComparisonDTO.ProductInfo.builder()
                    .isAllProducts(true)
                    .productName("전체 제품")
                    .build();
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));

            productInfo = ProductComparisonDTO.ProductInfo.builder()
                    .productId(product.getId())
                    .productCode(product.getProductCode())
                    .productName(product.getProductName())
                    .category(product.getCategory())
                    .isAllProducts(false)
                    .build();
        }

        List<ProductComparisonDTO.YearData> yearDataList = new ArrayList<>();

        for (Integer year : years) {
            List<ProductComparisonDTO.MonthData> monthDataList = new ArrayList<>();
            Integer yearQuantitySum = 0;
            BigDecimal yearAmountSum = BigDecimal.ZERO;

            for (int month = 1; month <= 12; month++) {
                ProductComparisonDTO.MonthData monthData = calculateMonthData(productId, year, month);

                monthDataList.add(monthData);
                yearQuantitySum += monthData.getQuantity();
                yearAmountSum = yearAmountSum.add(monthData.getAmount());
            }

            ProductComparisonDTO.MonthData yearTotal = ProductComparisonDTO.MonthData.builder()
                    .quantity(yearQuantitySum)
                    .amount(yearAmountSum)
                    .build();

            yearDataList.add(ProductComparisonDTO.YearData.builder()
                    .year(year)
                    .isCurrentYear(year == currentYear)
                    .months(monthDataList)
                    .yearTotal(yearTotal)
                    .build());
        }

        // 증감률 계산 (전년 대비 올해)
        ProductComparisonDTO.GrowthRateRow growthRates = calculateGrowthRates(yearDataList);

        return ProductComparisonDTO.builder()
                .productInfo(productInfo)
                .years(yearDataList)
                .growthRates(growthRates)
                .build();
    }

    /**
     * 특정 제품의 특정 년월 데이터 계산
     */
    private ProductComparisonDTO.MonthData calculateMonthData(Long productId, int year, int month) {
        List<DailySales> salesList;

        if (productId == null) {
            // 전체 제품 합계
            salesList = dailySalesRepository.findAllByYearAndMonth(year, month);
        } else {
            // 특정 제품
            salesList = dailySalesRepository
                    .findByProductIdAndYearAndMonthForComparison(productId, year, month);
        }

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DailySales sales : salesList) {
            totalQuantity += sales.getQuantity();

            LocalDateTime salesDateTime = sales.getSalesDate().atStartOfDay();
            Optional<ProductPriceHistory> priceHistory = productService
                    .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime);

            if (priceHistory.isPresent()) {
                BigDecimal amount = priceHistory.get().getSupplyPrice()
                        .multiply(BigDecimal.valueOf(sales.getQuantity()));
                totalAmount = totalAmount.add(amount);
            }
        }

        return ProductComparisonDTO.MonthData.builder()
                .month(month)
                .quantity(totalQuantity)
                .amount(totalAmount)
                .build();
    }

    /**
     * 증감률 계산 (전년 대비 올해, 금액 기준)
     */
    private ProductComparisonDTO.GrowthRateRow calculateGrowthRates(
            List<ProductComparisonDTO.YearData> yearDataList) {

        if (yearDataList.size() < 2) {
            return ProductComparisonDTO.GrowthRateRow.builder()
                    .monthlyGrowthRates(new ArrayList<>())
                    .totalGrowthRate(BigDecimal.ZERO)
                    .build();
        }

        ProductComparisonDTO.YearData previousYear = yearDataList.get(yearDataList.size() - 2);
        ProductComparisonDTO.YearData currentYear = yearDataList.get(yearDataList.size() - 1);

        List<BigDecimal> monthlyGrowthRates = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            BigDecimal prevAmount = previousYear.getMonths().get(i).getAmount();
            BigDecimal currAmount = currentYear.getMonths().get(i).getAmount();

            BigDecimal growthRate = calculateGrowthRate(prevAmount, currAmount);
            monthlyGrowthRates.add(growthRate);
        }

        BigDecimal totalGrowthRate = calculateGrowthRate(
                previousYear.getYearTotal().getAmount(),
                currentYear.getYearTotal().getAmount());

        return ProductComparisonDTO.GrowthRateRow.builder()
                .monthlyGrowthRates(monthlyGrowthRates)
                .totalGrowthRate(totalGrowthRate)
                .build();
    }

    private BigDecimal calculateGrowthRate(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : new BigDecimal("100");
        }

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}