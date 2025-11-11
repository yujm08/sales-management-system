package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.MonthlyComparisonDTO;
import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.entity.Product;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.MonthlyComparisonService;
import com.mynet.sales_management_system.service.ProductService;
import com.mynet.sales_management_system.service.StatisticsService;
import com.mynet.sales_management_system.service.ViewStatisticsService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 캐논 계정 전용 컨트롤러
 * - 조회만 가능 (수정/등록/삭제 권한 없음)
 * - 전체 통계만 표시 (버튼 없음)
 */
@Controller
@RequestMapping("/canon")
@RequiredArgsConstructor
@Slf4j
public class CanonController {

        private final ViewStatisticsService viewStatisticsService;
        private final CompanyRepository companyRepository;
        private final MonthlyComparisonService monthlyComparisonService;
        private final StatisticsService statisticsService;
        private final ProductService productService;

        /**
         * 조회 페이지 (캐논 메인 페이지)
         * 전체 통계만 표시, 수정 버튼 없음
         */
        @GetMapping("/view")
        public String viewPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                        @RequestParam(defaultValue = "all") String companyFilter,
                        @RequestParam(required = false) String date,
                        Model model) {

                LocalDate targetDate = (date != null && !date.isEmpty())
                                ? LocalDate.parse(date)
                                : DateUtil.getCurrentDate();

                // ViewStatisticsService를 사용해 통계 데이터 생성
                List<ViewStatisticsDTO> statisticsData = viewStatisticsService
                                .getViewStatistics(companyFilter, targetDate);

                // 제품 데이터만 카테고리별로 그룹화
                Map<String, List<ViewStatisticsDTO>> statisticsGroupedByCategory = statisticsData.stream()
                                .filter(item -> item.getProductCode() != null)
                                .collect(Collectors.groupingBy(
                                                ViewStatisticsDTO::getCategory,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                // 소계/합계 데이터 별도 추출
                Map<String, ViewStatisticsDTO> subtotalData = new HashMap<>();
                ViewStatisticsDTO grandTotalData = null;

                for (ViewStatisticsDTO item : statisticsData) {
                        if ("소계".equals(item.getCategory())) {
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
                model.addAttribute("subtotalData", subtotalData);
                model.addAttribute("grandTotalData", grandTotalData);
                model.addAttribute("subsidiaryCompanies", subsidiaryCompanies);
                model.addAttribute("targetDate", targetDate);
                model.addAttribute("companyFilter", companyFilter);
                model.addAttribute("currentMonth", targetDate.getMonthValue());

                log.info("Canon 조회 페이지 접근: 사용자={}, 필터={}, 날짜={}",
                                userDetails.getUsername(), companyFilter, targetDate);

                return "canon/view";
        }

        /**
         * 비교 - 월별 페이지
         */
        @GetMapping("/compare/monthly")
        public String compareMonthlyPage(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) String companyId,
                        Model model) {

                // 기본값 설정
                int currentYear = LocalDate.now().getYear();
                int selectedYear = (year != null) ? year : currentYear;

                // 연도 목록 생성 (최근 5년)
                List<Integer> years = new ArrayList<>();
                for (int y = currentYear; y >= currentYear - 4; y--) {
                        years.add(y);
                }

                // 회사 목록 (캐논 제외, 하위회사만)
                List<Company> companies = companyRepository.findByIsMynetFalse()
                                .stream()
                                .filter(company -> !"캐논".equals(company.getName()))
                                .collect(Collectors.toList());

                // companyId 파라미터 파싱 (문자열 "all" 처리)
                Long companyIdValue = null;
                if (companyId != null && !"all".equalsIgnoreCase(companyId.trim())) {
                        try {
                                companyIdValue = Long.parseLong(companyId);
                        } catch (NumberFormatException e) {
                                log.warn("잘못된 companyId 값: {}", companyId);
                                companyIdValue = null;
                        }
                }

                // 월별 비교 데이터 조회
                List<MonthlyComparisonDTO.CategoryData> monthlyData = monthlyComparisonService
                                .getMonthlyComparison(selectedYear, companyIdValue);

                // 전체 합계 계산
                MonthlyComparisonDTO.GrandTotal grandTotal = monthlyComparisonService.calculateGrandTotal(monthlyData);

                // 카테고리별 색상 인덱스 생성
                Map<String, Integer> categoryColorIndex = new LinkedHashMap<>();
                int colorIndex = 1;
                for (MonthlyComparisonDTO.CategoryData categoryData : monthlyData) {
                        categoryColorIndex.put(categoryData.getCategory(), colorIndex);
                        colorIndex = (colorIndex % 4) + 1;
                }

                // 모델에 데이터 전달 (HTML에서 요구하는 속성 이름 기준)
                model.addAttribute("years", years);
                model.addAttribute("selectedYear", selectedYear);
                model.addAttribute("companies", companies);
                model.addAttribute("selectedCompanyId", companyIdValue);
                model.addAttribute("monthlyData", monthlyData);
                model.addAttribute("grandTotal", grandTotal);
                model.addAttribute("categoryColorIndex", categoryColorIndex);
                model.addAttribute("companyName", userDetails.getCompanyName());

                log.info("캐논 월별 비교 조회: 년도={}, 회사ID={}", selectedYear, companyIdValue);

                return "canon/compare/monthly";
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

                return "canon/compare/yearly";
        }

        /**
         * 년도별 데이터 조회
         */
        @PreAuthorize("hasAnyRole('CANON')")
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

                return "canon/compare/period";
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

                return "canon/compare/product";
        }

}
