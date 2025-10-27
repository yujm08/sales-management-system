package com.mynet.sales_management_system.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductComparisonDTO {

    private ProductInfo productInfo;
    private List<YearData> years;
    private GrowthRateRow growthRates;

    // 제품 정보
    @Data
    @Builder
    public static class ProductInfo {
        private Long productId;
        private String productCode;
        private String productName;
        private String category;
        private boolean isAllProducts; // 전체 제품 합계 여부
    }

    // 연도별 데이터
    @Data
    @Builder
    public static class YearData {
        private Integer year;
        private boolean isCurrentYear;
        private List<MonthData> months;
        private MonthData yearTotal;
    }

    // 월별 수량/금액
    @Data
    @Builder
    public static class MonthData {
        private Integer month;
        private Integer quantity;
        private BigDecimal amount;
    }

    // 증감률 행
    @Data
    @Builder
    public static class GrowthRateRow {
        private List<BigDecimal> monthlyGrowthRates; // 12개월 증감률
        private BigDecimal totalGrowthRate; // 연간 합계 증감률
    }
}