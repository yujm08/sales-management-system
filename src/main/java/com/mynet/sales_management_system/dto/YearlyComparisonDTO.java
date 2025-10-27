package com.mynet.sales_management_system.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class YearlyComparisonDTO {
    
    // 카테고리별 데이터
    @Data
    @Builder
    public static class CategoryData {
        private String category;
        private List<ProductYearlyData> products;
        private YearlyTotal categoryTotal; // 카테고리별 합계
    }
    
    // 제품별 연도별 데이터
    @Data
    @Builder
    public static class ProductYearlyData {
        private Long productId;
        private String productCode;
        private String productName;
        private String category;
        
        private BigDecimal year1Amount;  // 전전년 연간 합계
        private BigDecimal year2Amount;  // 전년 연간 합계
        private BigDecimal year3Amount;  // 올해 연간 합계
        
        private BigDecimal growthRate;   // 증감률 (전년 대비 올해)
    }
    
    // 연도별 합계
    @Data
    @Builder
    public static class YearlyTotal {
        private BigDecimal year1Amount;
        private BigDecimal year2Amount;
        private BigDecimal year3Amount;
        private BigDecimal growthRate;
    }
    
    // 전체 합계
    @Data
    @Builder
    public static class GrandTotal {
        private BigDecimal year1Amount;
        private BigDecimal year2Amount;
        private BigDecimal year3Amount;
        private BigDecimal growthRate;
    }
}