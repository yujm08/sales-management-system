// MynetController.java - 마이넷 전용 컨트롤러
package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.DailySalesStatusDTO;
import com.mynet.sales_management_system.dto.MonthlyComparisonDTO;
import com.mynet.sales_management_system.dto.MynetQuantityUpdateRequest;
import com.mynet.sales_management_system.dto.MynetTargetUpdateRequest;
import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.DailySalesService;
import com.mynet.sales_management_system.service.MonthlyComparisonService;
import com.mynet.sales_management_system.service.ProductService;
import com.mynet.sales_management_system.service.SalesService;
import com.mynet.sales_management_system.service.TargetService;
import com.mynet.sales_management_system.service.ViewStatisticsService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마이넷 전용 컨트롤러
 * - 조회: 전체/개별 회사 데이터 조회 및 수정
 * - 비교: 월별/년도별/기간별 비교 분석
 * - 제품 분류: 제품 등록 및 관리
 */
@Controller
@RequestMapping("/mynet")
@RequiredArgsConstructor
@Slf4j
public class MynetController {

    private final SalesService salesService;
    private final ProductService productService;
    private final TargetService targetService;
    private final ViewStatisticsService viewStatisticsService;
    private final CompanyRepository companyRepository;
    private final MonthlyComparisonService monthlyComparisonService;
    private final DailySalesService dailySalesService;

    /**
     * 조회 페이지 (마이넷 메인 페이지)
     */

    @GetMapping("/view")
    public String viewPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "all") String companyFilter,
            @RequestParam(required = false) String date,
            Model model) {

        LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : DateUtil.getCurrentDate();

        // ViewStatisticsService를 사용해 통계 데이터 생성
        List<ViewStatisticsDTO> statisticsData = viewStatisticsService.getViewStatistics(companyFilter, targetDate);

        // 간단한 방법: 기존 Service 결과를 그대로 사용하고, HTML에서 분리
        // 제품 데이터만 카테고리별로 그룹화
        Map<String, List<ViewStatisticsDTO>> statisticsGroupedByCategory = statisticsData.stream()
                .filter(item -> item.getProductCode() != null) // 제품 데이터만
                .collect(Collectors.groupingBy(
                        ViewStatisticsDTO::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // 소계/합계 데이터 별도 추출
        Map<String, ViewStatisticsDTO> subtotalData = new HashMap<>();
        ViewStatisticsDTO grandTotalData = null;

        for (ViewStatisticsDTO item : statisticsData) {
            if ("소계".equals(item.getCategory())) {
                // 소계 데이터: productName에서 카테고리명 추출
                String categoryName = item.getProductName().replace(" 소계", "");
                subtotalData.put(categoryName, item);
            } else if ("합계".equals(item.getCategory())) {
                grandTotalData = item;
            }
        }

        // 하위 회사 목록 조회
        List<Company> subsidiaryCompanies = companyRepository.findByIsMynetFalse()
                .stream()
                .filter(company -> !"캐논".equals(company.getName()))
                .collect(Collectors.toList());

        model.addAttribute("statisticsData", statisticsGroupedByCategory);
        model.addAttribute("subtotalData", subtotalData); // 소계 데이터 별도 전달
        model.addAttribute("grandTotalData", grandTotalData);
        model.addAttribute("subsidiaryCompanies", subsidiaryCompanies);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("companyFilter", companyFilter);
        model.addAttribute("currentMonth", targetDate.getMonthValue());

        log.info("마이넷 조회 페이지 접근: 사용자={}, 필터={}, 날짜={}",
                userDetails.getUsername(), companyFilter, targetDate);

        return "mynet/view";
    }

    /**
     * AJAX 날짜 변경 요청 처리
     */
    @GetMapping("/view/ajax")
    @ResponseBody
    public Map<String, Object> getViewDataAjax(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "all") String companyFilter,
            @RequestParam String date) {

        LocalDate targetDate = LocalDate.parse(date);
        List<ViewStatisticsDTO> statisticsData = viewStatisticsService.getViewStatistics(companyFilter, targetDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("statisticsData", statisticsData);
        response.put("targetDate", targetDate.toString());
        response.put("currentMonth", targetDate.getMonthValue());

        return response;
    }

    /**
     * 수량 수정 - JSON 방식
     */
    @PostMapping("/update-quantity")
    @ResponseBody
    public Map<String, Object> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MynetQuantityUpdateRequest request) { // ✅ 새 DTO 사용

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("===== 수량 수정 요청 =====");
            log.info("요청 데이터: {}", request);

            // 파라미터 검증
            if (request.getCompanyId() == null || request.getCompanyId().isEmpty()) {
                response.put("success", false);
                response.put("message", "회사 ID가 필요합니다");
                return response;
            }

            if (request.getSalesDate() == null || request.getSalesDate().isEmpty()) {
                response.put("success", false);
                response.put("message", "날짜가 필요합니다");
                return response;
            }

            Long companyId = Long.parseLong(request.getCompanyId());
            LocalDate date = LocalDate.parse(request.getSalesDate());

            int successCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            // 각 제품별로 저장
            for (Map.Entry<Long, Integer> entry : request.getQuantities().entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                Map<String, Object> itemResult = new HashMap<>();
                itemResult.put("productId", productId);

                try {
                    salesService.saveDailySales(companyId, productId, date, quantity,
                            userDetails.getUsername());
                    itemResult.put("success", true);
                    successCount++;

                    log.info("제품 {} 수량 수정 성공: {}", productId, quantity);
                } catch (Exception e) {
                    itemResult.put("success", false);
                    itemResult.put("message", e.getMessage());
                    log.error("제품 {} 수량 수정 실패", productId, e);
                }
                results.add(itemResult);
            }

            response.put("success", true);
            response.put("successCount", successCount);
            response.put("totalCount", request.getQuantities().size());
            response.put("results", results);

            log.info("수량 수정 완료: 성공 {}/{}", successCount, request.getQuantities().size());

        } catch (NumberFormatException e) {
            log.error("companyId 파싱 실패: {}", request.getCompanyId(), e);
            response.put("success", false);
            response.put("message", "잘못된 회사 ID 형식입니다");
        } catch (Exception e) {
            log.error("수량 수정 실패", e);
            response.put("success", false);
            response.put("message", "서버 에러: " + e.getMessage());
        }

        return response;
    }

    /**
     * 목표 수정 - JSON 방식
     */
    @PostMapping("/update-target")
    @ResponseBody
    public Map<String, Object> updateTarget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MynetTargetUpdateRequest request) { // ✅ 새 DTO 사용

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("===== 목표 수정 요청 =====");
            log.info("요청 데이터: {}", request);

            if (request.getCompanyId() == null || request.getCompanyId().isEmpty()) {
                response.put("success", false);
                response.put("message", "회사 ID가 필요합니다");
                return response;
            }

            if (request.getTargetDate() == null || request.getTargetDate().isEmpty()) {
                response.put("success", false);
                response.put("message", "날짜가 필요합니다");
                return response;
            }

            Long companyId = Long.parseLong(request.getCompanyId());
            LocalDate date = LocalDate.parse(request.getTargetDate());

            int successCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (Map.Entry<Long, Integer> entry : request.getTargetQuantities().entrySet()) {
                Long productId = entry.getKey();
                Integer targetQuantity = entry.getValue();

                Map<String, Object> itemResult = new HashMap<>();
                itemResult.put("productId", productId);

                try {
                    targetService.saveTarget(companyId, productId, date.getYear(),
                            date.getMonthValue(), targetQuantity,
                            userDetails.getUsername());
                    itemResult.put("success", true);
                    successCount++;

                    log.info("제품 {} 목표 수정 성공: {}", productId, targetQuantity);
                } catch (Exception e) {
                    itemResult.put("success", false);
                    itemResult.put("message", e.getMessage());
                    log.error("제품 {} 목표 수정 실패", productId, e);
                }
                results.add(itemResult);
            }

            response.put("success", true);
            response.put("successCount", successCount);
            response.put("totalCount", request.getTargetQuantities().size());
            response.put("results", results);

            log.info("목표 수정 완료: 성공 {}/{}", successCount, request.getTargetQuantities().size());

        } catch (NumberFormatException e) {
            log.error("companyId 파싱 실패: {}", request.getCompanyId(), e);
            response.put("success", false);
            response.put("message", "잘못된 회사 ID 형식입니다");
        } catch (Exception e) {
            log.error("목표 수정 실패", e);
            response.put("success", false);
            response.put("message", "서버 에러: " + e.getMessage());
        }

        return response;
    }

    /**
     * 비교 - 월별 페이지
     */
    @GetMapping("/compare/monthly")
    public String compareMonthlyPage(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String companyId,
            Model model) {

        // 기본값 설정
        int selectedYear = year != null ? year : LocalDate.now().getYear();

        // 연도 목록 생성 (최근 5년)
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear; y >= currentYear - 4; y--) {
            years.add(y);
        }

        // 회사 목록
        List<Company> companies = companyRepository.findByIsMynetFalse()
                .stream()
                .filter(company -> !"캐논".equals(company.getName()))
                .collect(Collectors.toList());

        Long companyIdValue = null;
        if (companyId != null && !"all".equalsIgnoreCase(companyId.trim())) {
            try {
                companyIdValue = Long.parseLong(companyId);
            } catch (NumberFormatException e) {
                // 잘못된 형식이면 null로 처리 (전체 보기)
                companyIdValue = null;
            }
        }

        // 월별 데이터 조회
        List<MonthlyComparisonDTO.CategoryData> monthlyData = monthlyComparisonService
                .getMonthlyComparison(selectedYear, companyIdValue);

        // 전체 합계 계산
        MonthlyComparisonDTO.GrandTotal grandTotal = monthlyComparisonService.calculateGrandTotal(monthlyData);

        model.addAttribute("years", years);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("companies", companies);
        model.addAttribute("selectedCompanyId", companyIdValue);
        model.addAttribute("monthlyData", monthlyData);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("currentPage", "compare");
        model.addAttribute("comparePage", "monthly");

        return "mynet/compare/monthly";
    }

    /**
     * 비교 - 년도별 페이지
     */
    @GetMapping("/compare/yearly")
    public String compareYearlyPage(Model model) {
        List<Company> companies = companyRepository.findByIsMynetFalse()
                .stream()
                .filter(company -> !"캐논".equals(company.getName()))
                .collect(Collectors.toList());

        model.addAttribute("companies", companies);
        model.addAttribute("currentPage", "compare");
        model.addAttribute("comparePage", "yearly");

        return "mynet/compare/yearly";
    }

    /**
     * 비교 - 기간별 페이지
     */
    @GetMapping("/compare/period")
    public String comparePeriodPage(Model model) {
        List<Company> companies = companyRepository.findByIsMynetFalse()
                .stream()
                .filter(company -> !"캐논".equals(company.getName()))
                .collect(Collectors.toList());

        List<Product> products = productService.getAllProducts();

        model.addAttribute("companies", companies);
        model.addAttribute("products", products);
        model.addAttribute("currentPage", "compare");
        model.addAttribute("comparePage", "period");

        return "mynet/compare/period";
    }

    /**
     * 비교 - 제품별 페이지
     */
    @GetMapping("/compare/product")
    public String compareProductPage(Model model) {
        List<Product> products = productService.getAllProducts();

        model.addAttribute("products", products);
        model.addAttribute("currentPage", "compare");
        model.addAttribute("comparePage", "product");

        return "mynet/compare/product";
    }

    /**
     * 제품 분류 페이지
     */
    @GetMapping("/products")
    public String productsPage(Model model) {
        List<Product> allProducts = productService.getAllProducts();
        List<String> categories = productService.getCategories();

        model.addAttribute("products", allProducts);
        model.addAttribute("categories", categories);

        return "mynet/products";
    }

    /**
     * 제품 등록
     */
    @PostMapping("/products/create")
    @ResponseBody
    public String createProduct(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String productName,
            @RequestParam String category,
            @RequestParam BigDecimal costPrice,
            @RequestParam BigDecimal supplyPrice) {
        try {
            productService.createProduct(productName, category, costPrice, supplyPrice, userDetails.getUsername());

            log.info("제품 등록: 이름={}, 분류={}, 등록자={}", productName, category, userDetails.getUsername());
            return "success";
        } catch (Exception e) {
            log.error("제품 등록 실패", e);
            return "error:" + e.getMessage();
        }
    }

    /**
     * 제품 활성화 상태 변경
     */
    @PostMapping("/products/toggle-active")
    @ResponseBody
    public String toggleProductActive(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long productId,
            @RequestParam boolean isActive) {
        try {
            productService.updateProductActiveStatus(productId, isActive);

            log.info("제품 상태 변경: 제품ID={}, 활성화={}, 수정자={}",
                    productId, isActive, userDetails.getUsername());
            return "success";
        } catch (Exception e) {
            log.error("제품 상태 변경 실패", e);
            return "error:" + e.getMessage();
        }
    }

    /**
     * 일일매출현황 페이지
     */
    @GetMapping("/daily-sales")
    public String dailySalesPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String date,
            Model model) {

        LocalDate targetDate = (date != null && !date.isEmpty())
                ? LocalDate.parse(date)
                : DateUtil.getCurrentDate();

        // 일일매출현황 데이터 조회
        List<DailySalesStatusDTO> statusList = dailySalesService.getDailySalesStatus(targetDate);

        // 카테고리별로 그룹화
        Map<String, List<DailySalesStatusDTO>> dailySalesData = statusList.stream()
                .collect(Collectors.groupingBy(
                        DailySalesStatusDTO::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("dailySalesData", dailySalesData);
        model.addAttribute("targetDate", targetDate);

        log.info("일일매출현황 페이지 접근: 사용자={}, 날짜={}",
                userDetails.getUsername(), targetDate);

        return "mynet/daily-sales";
    }
}