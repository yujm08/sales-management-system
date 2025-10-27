// ViewStatisticsDTO.java - 마이넷 조회용 통합 데이터 DTO
package com.mynet.sales_management_system.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ViewStatisticsDTO {
    // 제품 기본 정보
    private Long productId;
    private String category;
    private String productCode;
    private String productName;
    private BigDecimal costPrice;
    private BigDecimal supplyPrice;
    private String categoryClass;

    // 일 합계 데이터
    private DailySummary dailySummary;

    // 월 합계 데이터 (선택된 날짜까지의)
    private MonthlySummary monthlySummary;

    // 목표 및 달성률
    private TargetData targetData;

    // 회사별 데이터 (전체 통계일 때)
    private List<CompanyData> companyDataList;

    @Data
    @Builder
    public static class DailySummary {
        private Integer quantity;
        private BigDecimal amount;
        private BigDecimal profit;
        private BigDecimal profitRate; // 이익률 (%)
        private ComparisonData comparison; // 전일 대비
        private LocalDateTime lastModifiedAt;
        private String modifiedBy;
    }

    @Data
    @Builder
    public static class MonthlySummary {
        private Integer quantity;
        private BigDecimal amount;
        private BigDecimal profit;
        private BigDecimal profitRate;
        private ComparisonData comparison; // 전월 대비
    }

    @Data
    @Builder
    public static class TargetData {
        private Integer targetMonth;
        private Integer targetQuantity;
        private BigDecimal achievementRate; // 달성률 (%)
    }

    @Data
    @Builder
    public static class CompanyData {
        private Long companyId;
        private String companyName;
        private Integer quantity;
        private BigDecimal amount;
        private BigDecimal profit;
        private LocalDateTime lastModifiedAt;
        private String modifiedBy;
    }

    @Data
    @Builder
    public static class ComparisonData {
        private BigDecimal previousValue;
        private BigDecimal currentValue;
        private BigDecimal changeAmount;
        private BigDecimal changeRate; // 변화율 (%)
        private boolean isIncrease;
    }
}