package com.mynet.sales_management_system.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlyComparisonDTO {
    
    // 카테고리별 데이터
    @Data
    @Builder
    public static class CategoryData {
        private String category;
        private List<ProductMonthlyData> products;
    }
    
    // 제품별 월별 데이터
    @Data
    @Builder
    public static class ProductMonthlyData {
        private Long productId;
        private String productCode;
        private String productName;
        private List<MonthData> monthlyData; // 12개월 데이터 (0:1월 ~ 11:12월)
        private Integer totalQuantity;
        private BigDecimal totalAmount;
    }
    
    // 월별 수량/금액
    @Data
    @Builder
    public static class MonthData {
        private Integer month;
        private Integer quantity;
        private BigDecimal amount;
    }
    
    // 전체 합계
    @Data
    @Builder
    public static class GrandTotal {
        private List<MonthData> monthlyTotals; // 12개월 합계
        private Integer totalQuantity;
        private BigDecimal totalAmount;
    }
}
