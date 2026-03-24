package com.mynet.sales_management_system.service;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.DailySalesRepository;
import com.mynet.sales_management_system.repository.ProductRepository;

import com.mynet.sales_management_system.entity.Product;
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