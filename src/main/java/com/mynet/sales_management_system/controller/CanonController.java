package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.dto.ViewStatisticsDTO;
import com.mynet.sales_management_system.entity.Company;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.ViewStatisticsService;
import com.mynet.sales_management_system.repository.CompanyRepository;
import com.mynet.sales_management_system.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
