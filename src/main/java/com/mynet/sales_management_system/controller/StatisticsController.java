package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.PeriodComparisonDTO;
import com.mynet.sales_management_system.dto.ProductComparisonDTO;
import com.mynet.sales_management_system.dto.ProductDTO;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.entity.ProductPriceHistory;
import com.mynet.sales_management_system.repository.ProductRepository;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.PeriodComparisonExcelService;
import com.mynet.sales_management_system.service.PeriodComparisonService;
import com.mynet.sales_management_system.service.ProductComparisonExcelService;
import com.mynet.sales_management_system.service.ProductComparisonService;
import com.mynet.sales_management_system.service.ProductService;
import com.mynet.sales_management_system.service.StatisticsService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final PeriodComparisonService periodComparisonService;
    private final ProductService productService;
    private final ProductComparisonService productComparisonService;
    private final PeriodComparisonExcelService periodComparisonExcelService;
    private final ProductComparisonExcelService productComparisonExcelService;
    private final ProductRepository productRepository;

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

    /**
     * 기간별 비교 엑셀 다운로드
     */
    @PostMapping("/period-comparison/download")
    public ResponseEntity<byte[]> downloadPeriodComparisonExcel(
            @RequestBody PeriodComparisonRequest request) {

        try {
            List<PeriodComparisonDTO.PeriodData> periodsData = new ArrayList<>();

            for (PeriodComparisonRequest.Period period : request.getPeriods()) {
                PeriodComparisonDTO.PeriodData periodData;

                if (request.getProductId() != null) {
                    periodData = periodComparisonService.getPeriodDataForProduct(
                            request.getProductId(),
                            period.getStartDate(),
                            period.getEndDate());
                } else {
                    periodData = periodComparisonService.getPeriodDataForAllProducts(
                            period.getStartDate(),
                            period.getEndDate());
                }

                periodsData.add(periodData);
            }

            // 제품명 조회
            String productName = "전체 제품";
            if (request.getProductId() != null) {
                productName = productRepository.findById(request.getProductId())
                        .map(Product::getProductName)
                        .orElse("전체 제품");
            }

            // 엑셀 생성
            byte[] excelData = periodComparisonExcelService.generatePeriodComparisonExcel(
                    periodsData, productName);

            // 파일명 생성
            String fileName = String.format("기간별비교_%s.xlsx", productName);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IOException e) {
            log.error("기간별 비교 엑셀 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 제품별 비교 엑셀 다운로드
     */
    @GetMapping("/product-comparison/download")
    public ResponseEntity<byte[]> downloadProductComparisonExcel(
            @RequestParam(required = false) Long productId) {

        try {
            // 데이터 조회
            ProductComparisonDTO comparisonData = productComparisonService.getProductComparison(productId);

            // 엑셀 생성
            byte[] excelData = productComparisonExcelService.generateProductComparisonExcel(comparisonData);

            // 파일명 생성
            String productName = comparisonData.getProductInfo().isAllProducts()
                    ? "전체제품"
                    : comparisonData.getProductInfo().getProductName();
            String fileName = String.format("제품별비교_%s.xlsx", productName);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IOException e) {
            log.error("제품별 비교 엑셀 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}