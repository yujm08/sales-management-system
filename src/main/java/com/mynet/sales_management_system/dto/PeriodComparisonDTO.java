package com.mynet.sales_management_system.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PeriodComparisonDTO {

    // 기간별 데이터
    @Data
    @Builder
    public static class PeriodData {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalQuantity; // 기간 합계 수량
        private BigDecimal totalAmount; // 기간 합계 금액
        private List<DailyData> dailyDetails; // 일별 상세 데이터
        private boolean expanded; // 상세 보기 펼침 상태 (프론트용)
    }

    // 일별 데이터
    @Data
    @Builder
    public static class DailyData {
        private LocalDate date;
        private Integer quantity;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class ComparisonResult {
        private List<String> periodLabels; // 헤더용
        private List<CategoryData> categories;
        private List<PeriodAmount> grandTotal; // 기간별 전체 합계
    }

    @Data
    @Builder
    public static class CategoryData {
        private String category;
        private List<ProductPeriodData> products;
        private List<PeriodAmount> subtotal; // 기간별 카테고리 소계
    }

    /** 제품 1개 × N개 기간 */
    @Data
    @Builder
    public static class ProductPeriodData {
        private String productCode;
        private String productName;
        private String category;
        private List<PeriodAmount> periodAmounts; // 기간 순서대로
    }

    /** 단일 기간의 수량 + 금액 */
    @Data
    @Builder
    public static class PeriodAmount {
        private Integer quantity;
        private BigDecimal amount;
    }
}