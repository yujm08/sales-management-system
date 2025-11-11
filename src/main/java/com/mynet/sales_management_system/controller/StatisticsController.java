package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import com.mynet.sales_management_system.dto.ProductComparisonDTO;
import com.mynet.sales_management_system.dto.ProductDTO;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.PeriodComparisonService;
import com.mynet.sales_management_system.service.ProductComparisonService;
import com.mynet.sales_management_system.service.ProductService;
import com.mynet.sales_management_system.service.StatisticsService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final PeriodComparisonService periodComparisonService;
    private final ProductService productService;
    private final ProductComparisonService productComparisonService;

    /**
     * 마이넷 측 비교탭 - 년도별 데이터 조회
     */
    @PreAuthorize("hasAnyRole('MYNET')")
    @GetMapping("/yearly-comparison")
    public ResponseEntity<StatisticsService.YearlyComparisonResponse> getYearlyComparison(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        int currentYear = LocalDate.now().getYear();

        StatisticsService.YearlyComparisonResponse response = statisticsService
                .getYearlyComparisonData(currentYear - 2, currentYear);

        log.info("년도별 비교 데이터 조회: {}년 ~ {}년", currentYear - 2, currentYear);

        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 비교 데이터 조회 API
     */
    @PreAuthorize("hasAnyRole('MYNET','CANON')")
    @PostMapping("/period-comparison")
    public ResponseEntity<List<PeriodComparisonDTO.PeriodData>> getPeriodComparison(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PeriodComparisonRequest request) {

        List<PeriodComparisonDTO.PeriodData> result = new ArrayList<>();

        for (PeriodComparisonRequest.Period period : request.getPeriods()) {
            PeriodComparisonDTO.PeriodData periodData;

            if (request.getProductId() == null) {
                // 전체 제품 합계
                periodData = periodComparisonService.getPeriodDataForAllProducts(
                        period.getStartDate(), period.getEndDate());
            } else {
                // 특정 제품
                periodData = periodComparisonService.getPeriodDataForProduct(
                        request.getProductId(), period.getStartDate(), period.getEndDate());
            }

            result.add(periodData);
        }

        log.info("기간별 비교 데이터 조회: 제품ID={}, 기간 수={}",
                request.getProductId(), request.getPeriods().size());

        return ResponseEntity.ok(result);
    }

    // 요청 DTO
    @Data
    public static class PeriodComparisonRequest {
        private Long productId; // null이면 전체 제품
        private List<Period> periods;

        @Data
        public static class Period {
            private LocalDate startDate;
            private LocalDate endDate;
        }
    }

    /**
     * 제품 목록 조회 API (기간별 비교용)
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<Product> products = productService.getAllProducts();

        List<ProductDTO> productDTOs = products.stream()
                .map(product -> {
                    ProductDTO dto = new ProductDTO();
                    dto.setId(product.getId());
                    dto.setProductCode(product.getProductCode());
                    dto.setProductName(product.getProductName());
                    dto.setCategory(product.getCategory());
                    dto.setActive(product.getIsActive());

                    // 현재 가격 조회
                    ProductPriceHistory currentPrice = productService
                            .getCurrentProductPrice(product.getId())
                            .orElse(null);

                    if (currentPrice != null) {
                        dto.setCurrentCostPrice(currentPrice.getCostPrice());
                        dto.setCurrentSupplyPrice(currentPrice.getSupplyPrice());
                    } else {
                        dto.setCurrentCostPrice(BigDecimal.ZERO);
                        dto.setCurrentSupplyPrice(BigDecimal.ZERO);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(productDTOs);
    }

    /**
     * 제품별 비교 데이터 조회
     */
    @PreAuthorize("hasAnyRole('MYNET','CANON')")
    @GetMapping("/product-comparison")
    public ResponseEntity<ProductComparisonDTO> getProductComparison(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long productId) {

        ProductComparisonDTO comparisonData = productComparisonService
                .getProductComparison(productId);

        log.info("제품별 비교 데이터 조회: 제품ID={}", productId);

        return ResponseEntity.ok(comparisonData);
    }

}