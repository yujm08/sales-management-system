package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.MonthlyComparisonDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MonthlyComparisonService {

    private final DailySalesRepository dailySalesRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * 연도별 월별 비교 데이터 생성
     */
    public List<MonthlyComparisonDTO.CategoryData> getMonthlyComparison(int year, Long companyId) {
        // 활성화된 제품 목록
        List<Product> products = productRepository.findByIsActiveTrueOrderByCategoryAscProductCodeAsc();

        // 카테고리별로 그룹화
        Map<String, List<Product>> productsByCategory = products.stream()
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<MonthlyComparisonDTO.CategoryData> result = new ArrayList<>();

        // 각 카테고리별로 처리
        for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
            String category = entry.getKey();
            List<Product> categoryProducts = entry.getValue();

            List<MonthlyComparisonDTO.ProductMonthlyData> productDataList = new ArrayList<>();

            for (Product product : categoryProducts) {
                // 12개월 데이터 생성
                List<MonthlyComparisonDTO.MonthData> monthlyData = new ArrayList<>();
                Integer totalQuantity = 0;
                BigDecimal totalAmount = BigDecimal.ZERO;

                for (int month = 1; month <= 12; month++) {
                    // 해당 월의 판매 데이터 조회
                    List<DailySales> monthlySales;

                    if (companyId == null || "all".equals(companyId.toString())) {
                        // 전체 회사
                        monthlySales = dailySalesRepository.findByProductIdAndYearAndMonth(
                                product.getId(), year, month, 31);
                    } else {
                        // 특정 회사
                        monthlySales = dailySalesRepository.findByCompanyIdAndProductIdAndYearAndMonth(
                                companyId, product.getId(), year, month, 31);
                    }

                    // 수량 합계
                    int monthQuantity = monthlySales.stream()
                            .mapToInt(DailySales::getQuantity)
                            .sum();

                    // 금액은 각 판매일의 가격으로 계산
                    BigDecimal monthAmount = BigDecimal.ZERO;
                    for (DailySales sales : monthlySales) {
                        LocalDateTime salesDateTime = sales.getSalesDate().atTime(23, 59, 59);
                        ProductPriceHistory priceHistory = productService
                                .getProductPriceAtDate(sales.getProduct().getId(), salesDateTime)
                                .orElse(null);

                        if (priceHistory != null) {
                            BigDecimal amount = priceHistory.getSupplyPrice()
                                    .multiply(BigDecimal.valueOf(sales.getQuantity()));
                            monthAmount = monthAmount.add(amount);
                        }
                    }

                    monthlyData.add(MonthlyComparisonDTO.MonthData.builder()
                            .month(month)
                            .quantity(monthQuantity)
                            .amount(monthAmount)
                            .build());

                    totalQuantity += monthQuantity;
                    totalAmount = totalAmount.add(monthAmount);
                }

                productDataList.add(MonthlyComparisonDTO.ProductMonthlyData.builder()
                        .productId(product.getId())
                        .productCode(product.getProductCode())
                        .productName(product.getProductName())
                        .monthlyData(monthlyData)
                        .totalQuantity(totalQuantity)
                        .totalAmount(totalAmount)
                        .build());
            }

            result.add(MonthlyComparisonDTO.CategoryData.builder()
                    .category(category)
                    .products(productDataList)
                    .build());
        }

        return result;
    }

    /**
     * 전체 합계 계산
     */
    public MonthlyComparisonDTO.GrandTotal calculateGrandTotal(
            List<MonthlyComparisonDTO.CategoryData> categoryDataList) {

        List<MonthlyComparisonDTO.MonthData> monthlyTotals = new ArrayList<>();
        Integer totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 12개월 각각 합계 계산
        for (int month = 1; month <= 12; month++) {
            int monthQuantity = 0;
            BigDecimal monthAmount = BigDecimal.ZERO;

            for (MonthlyComparisonDTO.CategoryData categoryData : categoryDataList) {
                for (MonthlyComparisonDTO.ProductMonthlyData productData : categoryData.getProducts()) {
                    MonthlyComparisonDTO.MonthData monthData = productData.getMonthlyData().get(month - 1);
                    monthQuantity += monthData.getQuantity();
                    monthAmount = monthAmount.add(monthData.getAmount());
                }
            }

            monthlyTotals.add(MonthlyComparisonDTO.MonthData.builder()
                    .month(month)
                    .quantity(monthQuantity)
                    .amount(monthAmount)
                    .build());

            totalQuantity += monthQuantity;
            totalAmount = totalAmount.add(monthAmount);
        }

        return MonthlyComparisonDTO.GrandTotal.builder()
                .monthlyTotals(monthlyTotals)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }
}