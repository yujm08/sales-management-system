package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;

import com.mynet.sales_management_system.entity.Product;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PeriodComparisonService {

        private final DailySalesRepository dailySalesRepository;
        private final ProductService productService;
        private final ProductRepository productRepository;

        /**
         * 특정 제품의 기간별 비교 데이터 조회
         */
        public PeriodComparisonDTO.PeriodData getPeriodDataForProduct(
                        Long productId, LocalDate startDate, LocalDate endDate) {

                // 전체 하위회사의 해당 제품 데이터 조회
                List<DailySales> salesList = dailySalesRepository
                                .findByProductIdAndDateRange(productId, startDate, endDate);

                return calculatePeriodData(salesList, startDate, endDate);
        }

        /**
         * 전체 제품의 기간별 비교 데이터 조회 (제품 미선택 시)
         */
        public PeriodComparisonDTO.PeriodData getPeriodDataForAllProducts(
                        LocalDate startDate, LocalDate endDate) {

                // 전체 하위회사, 전체 제품 데이터 조회
                List<DailySales> salesList = dailySalesRepository
                                .findByDateRangeOrderByDateAndProduct(startDate, endDate);

                return calculatePeriodData(salesList, startDate, endDate);
        }

        /**
         * 기간 데이터 계산 (일별 상세 포함)
         */
        private PeriodComparisonDTO.PeriodData calculatePeriodData(
                        List<DailySales> salesList, LocalDate startDate, LocalDate endDate) {

                // 날짜별로 그룹화
                Map<LocalDate, List<DailySales>> salesByDate = salesList.stream()
                                .collect(Collectors.groupingBy(DailySales::getSalesDate));

                List<PeriodComparisonDTO.DailyData> dailyDetails = new ArrayList<>();
                Integer totalQuantity = 0;
                BigDecimal totalAmount = BigDecimal.ZERO;

                // 날짜 범위를 순회하며 일별 데이터 생성
                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                        List<DailySales> daySales = salesByDate.getOrDefault(currentDate, new ArrayList<>());

                        int dayQuantity = 0;
                        BigDecimal dayAmount = BigDecimal.ZERO;

                        for (DailySales sales : daySales) {
                                dayQuantity += sales.getQuantity();

                                // 해당 날짜의 가격으로 금액 계산
                                ProductPriceHistory priceHistory = productService
                                                .getProductPriceAtDate(sales.getProduct().getId(),
                                                                currentDate.atStartOfDay())
                                                .orElse(null);

                                if (priceHistory != null) {
                                        BigDecimal amount = priceHistory.getSupplyPrice()
                                                        .multiply(BigDecimal.valueOf(sales.getQuantity()));
                                        dayAmount = dayAmount.add(amount);
                                }
                        }

                        dailyDetails.add(PeriodComparisonDTO.DailyData.builder()
                                        .date(currentDate)
                                        .quantity(dayQuantity)
                                        .amount(dayAmount)
                                        .build());

                        totalQuantity += dayQuantity;
                        totalAmount = totalAmount.add(dayAmount);

                        currentDate = currentDate.plusDays(1);
                }

                return PeriodComparisonDTO.PeriodData.builder()
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalQuantity(totalQuantity)
                                .totalAmount(totalAmount)
                                .dailyDetails(dailyDetails)
                                .expanded(false)
                                .build();
        }

        /**
         * 다기간 × 전제품 비교
         */
        public PeriodComparisonDTO.ComparisonResult getMultiPeriodComparison(
                        List<LocalDate[]> periods) {

                // 1. 활성 제품 카테고리 순으로 조회
                List<Product> activeProducts = productRepository
                                .findByIsActiveTrueOrderByCategoryAscProductCodeAsc();

                // 2. 기간 라벨 생성
                List<String> periodLabels = periods.stream()
                                .map(p -> formatDate(p[0]) + " ~ " + formatDate(p[1]))
                                .collect(Collectors.toList());

                // 3. 카테고리별 그룹화
                Map<String, List<Product>> byCategory = activeProducts.stream()
                                .collect(Collectors.groupingBy(
                                                Product::getCategory,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                // 4. 전체 합계 누적용
                List<PeriodComparisonDTO.PeriodAmount> grandTotal = initPeriodAmounts(periods.size());

                List<PeriodComparisonDTO.CategoryData> categoryDataList = new ArrayList<>();

                for (Map.Entry<String, List<Product>> entry : byCategory.entrySet()) {
                        String category = entry.getKey();
                        List<Product> products = entry.getValue();

                        List<PeriodComparisonDTO.PeriodAmount> subtotal = initPeriodAmounts(periods.size());
                        List<PeriodComparisonDTO.ProductPeriodData> productDataList = new ArrayList<>();

                        for (Product product : products) {
                                List<PeriodComparisonDTO.PeriodAmount> periodAmounts = new ArrayList<>();

                                for (int i = 0; i < periods.size(); i++) {
                                        LocalDate start = periods.get(i)[0];
                                        LocalDate end = periods.get(i)[1];

                                        List<DailySales> salesList = dailySalesRepository
                                                        .findByProductIdAndDateRange(product.getId(), start, end);

                                        int qty = 0;
                                        BigDecimal amt = BigDecimal.ZERO;

                                        for (DailySales sales : salesList) {
                                                qty += sales.getQuantity();

                                                Optional<ProductPriceHistory> price = productService
                                                                .getProductPriceAtDate(product.getId(),
                                                                                sales.getSalesDate().atStartOfDay());
                                                if (price.isPresent()) {
                                                        amt = amt.add(price.get().getSupplyPrice()
                                                                        .multiply(BigDecimal
                                                                                        .valueOf(sales.getQuantity())));
                                                }
                                        }

                                        periodAmounts.add(PeriodComparisonDTO.PeriodAmount.builder()
                                                        .quantity(qty).amount(amt).build());

                                        // 소계 누적
                                        subtotal.get(i).setQuantity(subtotal.get(i).getQuantity() + qty);
                                        subtotal.get(i).setAmount(subtotal.get(i).getAmount().add(amt));
                                        // 전체 합계 누적
                                        grandTotal.get(i).setQuantity(grandTotal.get(i).getQuantity() + qty);
                                        grandTotal.get(i).setAmount(grandTotal.get(i).getAmount().add(amt));
                                }

                                productDataList.add(PeriodComparisonDTO.ProductPeriodData.builder()
                                                .productCode(product.getProductCode())
                                                .productName(product.getProductName())
                                                .category(category)
                                                .periodAmounts(periodAmounts)
                                                .build());
                        }

                        categoryDataList.add(PeriodComparisonDTO.CategoryData.builder()
                                        .category(category)
                                        .products(productDataList)
                                        .subtotal(subtotal)
                                        .build());
                }

                return PeriodComparisonDTO.ComparisonResult.builder()
                                .periodLabels(periodLabels)
                                .categories(categoryDataList)
                                .grandTotal(grandTotal)
                                .build();
        }

        /** 기간 수만큼 0으로 초기화된 PeriodAmount 리스트 생성 */
        private List<PeriodComparisonDTO.PeriodAmount> initPeriodAmounts(int size) {
                List<PeriodComparisonDTO.PeriodAmount> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                        list.add(PeriodComparisonDTO.PeriodAmount.builder()
                                        .quantity(0).amount(BigDecimal.ZERO).build());
                }
                return list;
        }

        private String formatDate(LocalDate date) {
                return date.getYear() + "/" + date.getMonthValue() + "/" + date.getDayOfMonth();
        }

}