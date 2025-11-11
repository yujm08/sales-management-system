// SubsidiaryController.java - 하위회사 전용 컨트롤러
package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.ProductInputDTO;
import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.MonthlyComparisonService;
import com.mynet.sales_management_system.service.ProductService;
import com.mynet.sales_management_system.service.SalesService;
import com.mynet.sales_management_system.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.stream.Collectors;

import com.mynet.sales_management_system.dto.InputSaveRequest;
import com.mynet.sales_management_system.dto.MonthlyComparisonDTO;
import com.mynet.sales_management_system.service.ViewStatisticsService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 하위회사 전용 컨트롤러
 * - 입력 테이블 (수량만 입력 가능)
 * - 통계 테이블 (자사 데이터만)
 * - 월별매출집계 (당년만)
 */
@Controller
@RequestMapping("/subsidiary")
@RequiredArgsConstructor
@Slf4j
public class SubsidiaryController {

    private final SalesService salesService;
    private final ProductService productService;
    private final ViewStatisticsService viewStatisticsService;
    private final MonthlyComparisonService monthlyComparisonService;

    /**
     * 입력 테이블 페이지 (하위회사 메인 페이지)
     */
    @GetMapping("/input")
    public String inputPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String date,
            Model model) {

        Long companyId = userDetails.getCompanyId();
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();

        // 활성화된 모든 제품을 카테고리별로 그룹화하여 조회
        Map<String, List<ProductInputDTO>> productsByCategory = productService
                .getProductsForInput(companyId, targetDate);

        // 현재 월인지 확인
        boolean isCurrentMonth = targetDate.getYear() == LocalDate.now().getYear()
                && targetDate.getMonth() == LocalDate.now().getMonth();

        // 총 수량 계산
        int totalQuantity = productsByCategory.values().stream()
                .flatMap(List::stream)
                .mapToInt(ProductInputDTO::getQuantity)
                .sum();

        model.addAttribute("productsByCategory", productsByCategory);
        model.addAttribute("currentDate", targetDate);
        model.addAttribute("isCurrentMonth", isCurrentMonth);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("companyName", userDetails.getCompanyName());

        log.info("하위회사 입력 페이지 접근: 회사={}, 날짜={}", userDetails.getCompanyName(), targetDate);
        return "subsidiary/input";
    }

    /**
     * 수량 데이터 저장
     */
    @PostMapping("/save-quantity")
    @ResponseBody
    public String saveQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long productId,
            @RequestParam String salesDate,
            @RequestParam Integer quantity) {
        try {
            Long companyId = userDetails.getCompanyId();
            LocalDate date = LocalDate.parse(salesDate);

            // 당월 데이터만 수정 가능
            if (!DateUtil.isCurrentMonth(date)) {
                return "error:당월 데이터만 수정 가능합니다.";
            }

            salesService.saveDailySales(companyId, productId, date, quantity, userDetails.getUsername());

            log.info("수량 데이터 저장: 회사={}, 제품ID={}, 날짜={}, 수량={}",
                    userDetails.getCompanyName(), productId, date, quantity);

            return "success";
        } catch (Exception e) {
            log.error("수량 저장 실패", e);
            return "error:" + e.getMessage();
        }
    }

    /**
     * 일괄 수량 데이터 저장
     */
    @PostMapping("/input/save")
    @ResponseBody
    public ResponseEntity<?> saveBulkQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InputSaveRequest request) {
        try {
            Long companyId = userDetails.getCompanyId();
            LocalDate date = LocalDate.parse(request.getDate()); // String을 LocalDate로 변환

            // 당월 데이터만 수정 가능
            if (!DateUtil.isCurrentMonth(date)) {
                return ResponseEntity.badRequest().body("당월 데이터만 수정 가능합니다.");
            }

            // 각 제품별로 저장
            for (Map.Entry<Long, Integer> entry : request.getQuantities().entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                salesService.saveDailySales(companyId, productId, date, quantity, userDetails.getUsername());
            }

            log.info("일괄 수량 데이터 저장 완료: 회사={}, 날짜={}, 제품 수={}",
                    userDetails.getCompanyName(), date, request.getQuantities().size());

            return ResponseEntity.ok("저장되었습니다.");
        } catch (Exception e) {
            log.error("일괄 저장 실패", e);
            return ResponseEntity.status(500).body("저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 통계 조회 페이지 (하위회사 전용)
     */
    @GetMapping("/sales-summary")
    public String statisticsPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String date,
            Model model) {

        Long companyId = userDetails.getCompanyId();
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();

        List<ViewStatisticsDTO> statisticsData = viewStatisticsService
                .getViewStatistics(companyId.toString(), targetDate);

        // 카테고리별 제품 수 계산
        Map<String, Long> categoryCount = statisticsData.stream()
                .filter(item -> !"소계".equals(item.getCategory()) && !"합계".equals(item.getCategory()))
                .collect(Collectors.groupingBy(
                        ViewStatisticsDTO::getCategory,
                        LinkedHashMap::new, // 순서 유지
                        Collectors.counting()));

        // 카테고리별 색상 인덱스 맵 생성
        Map<String, Integer> categoryColorIndex = new LinkedHashMap<>();
        int colorIndex = 1;
        for (String category : categoryCount.keySet()) {
            categoryColorIndex.put(category, colorIndex);
            colorIndex = (colorIndex % 4) + 1;
        }

        model.addAttribute("statisticsData", statisticsData);
        model.addAttribute("categoryCount", categoryCount);
        model.addAttribute("categoryColorIndex", categoryColorIndex);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("companyName", userDetails.getCompanyName());
        model.addAttribute("currentMonth", targetDate.getMonthValue());

        log.info("하위회사 통계 조회: 회사ID={}, 날짜={}", companyId, targetDate);

        return "subsidiary/statistics";
    }

    /**
     * 월별매출집계 페이지 (당년 데이터만)
     */
    @GetMapping("/monthly")
    public String monthlyPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long companyId = userDetails.getCompanyId();
        int currentYear = LocalDate.now().getYear();

        // 기존 서비스 재사용
        List<MonthlyComparisonDTO.CategoryData> monthlyData = monthlyComparisonService
                .getMonthlyComparison(currentYear, companyId);

        // 전체 합계 계산
        MonthlyComparisonDTO.GrandTotal grandTotal = monthlyComparisonService
                .calculateGrandTotal(monthlyData);

        // 카테고리별 색상 인덱스 생성 (statistics.html과 동일한 로직)
        Map<String, Integer> categoryColorIndex = new LinkedHashMap<>();
        int colorIndex = 1;
        for (MonthlyComparisonDTO.CategoryData categoryData : monthlyData) {
            categoryColorIndex.put(categoryData.getCategory(), colorIndex);
            colorIndex = (colorIndex % 4) + 1;
        }

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("companyName", userDetails.getCompanyName());
        model.addAttribute("monthlyData", monthlyData);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("categoryColorIndex", categoryColorIndex);

        log.info("하위회사 월별매출집계 조회: 회사ID={}, 년도={}", companyId, currentYear);

        return "subsidiary/monthly";
    }
}