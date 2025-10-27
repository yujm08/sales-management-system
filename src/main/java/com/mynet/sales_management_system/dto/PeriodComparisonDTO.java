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
        private Integer totalQuantity;      // 기간 합계 수량
        private BigDecimal totalAmount;     // 기간 합계 금액
        private List<DailyData> dailyDetails; // 일별 상세 데이터
        private boolean expanded;           // 상세 보기 펼침 상태 (프론트용)
    }
    
    // 일별 데이터
    @Data
    @Builder
    public static class DailyData {
        private LocalDate date;
        private Integer quantity;
        private BigDecimal amount;
    }
}