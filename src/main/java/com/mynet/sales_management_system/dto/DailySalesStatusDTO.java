package com.mynet.sales_management_system.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 일일매출현황 데이터 전송 객체
 * - 제품별 회사별 일일/월간 판매 현황
 * - 목표 달성 및 전년도 비교 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesStatusDTO {
    private String category; // 카테고리
    private String productCode; // 제품코드
    private String productName; // 제품명
    private BigDecimal supplyPrice; // 공급가

    // 회사별 데이터 (영현아이앤씨, 마이씨앤에스, ...)
    @Builder.Default
    private Map<String, CompanySalesData> companySalesMap = new LinkedHashMap<>();

    // 일 합계
    private SalesData dailyTotal;

    // 월 합계
    private SalesData monthlyTotal;

    // N월 목표수량
    private Integer targetQuantity;

    // 비교 데이터
    private Integer prevMonthSales; // 전월 판매량
    private Integer yearToDateSales; // M년 (N-1)+N월 판매량
    private Integer lastYearPrevMonthSales; // (M-1)년 (N-1)월 판매량
    private Integer lastYearCurrentMonthSales; // (M-1)년 N월 판매량
    private Integer lastYearYearToDateSales; // (M-1)년 (N-1)+N월 판매량

    /**
     * 회사별 판매 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanySalesData {
        private Integer dailyQuantity; // 일 수량
        private Integer monthlyQuantity; // 월 수량
        private BigDecimal amount; // 금액
    }

    /**
     * 합계 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesData {
        private Integer quantity;
        private BigDecimal amount;
    }
}