// CanonController.java - 캐논 전용 컨트롤러
package com.mynet.sales_management_system.controller;

import com.mynet.sales_management_system.entity.DailySales;
import com.mynet.sales_management_system.security.CustomUserDetails;
import com.mynet.sales_management_system.service.SalesService;
import com.mynet.sales_management_system.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    private final SalesService salesService;

    /**
     * 조회 페이지 (캐논 메인 페이지)
     * 전체 통계만 표시, 수정 버튼 없음
     */
    @GetMapping("/view")
    public String viewPage(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "today") String date,
            Model model) {

        LocalDate targetDate = "today".equals(date) ? DateUtil.getCurrentDate() : LocalDate.parse(date);

        // 전체 회사 데이터 조회
        List<DailySales> salesData = salesService.getAllDailySalesByDate(targetDate);

        // 통계 계산
        // TODO: 캐논용 통계 데이터 계산

        model.addAttribute("salesData", salesData);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("isCanonAccount", true); // 캐논 계정 플래그

        log.info("캐논 조회 페이지 접근: 날짜={}", targetDate);
        return "canon/view";
    }

    /**
     * 비교 - 월별 페이지 (조회만)
     */
    @GetMapping("/compare/monthly")
    public String compareMonthlyPage(Model model) {
        model.addAttribute("isCanonAccount", true);
        return "canon/compare-monthly";
    }

    /**
     * 비교 - 년도별 페이지 (조회만)
     */
    @GetMapping("/compare/yearly")
    public String compareYearlyPage(Model model) {
        model.addAttribute("isCanonAccount", true);
        return "canon/compare-yearly";
    }

    /**
     * 비교 - 기간별 페이지 (조회만)
     */
    @GetMapping("/compare/period")
    public String comparePeriodPage(Model model) {
        model.addAttribute("isCanonAccount", true);
        return "canon/compare-period";
    }
}