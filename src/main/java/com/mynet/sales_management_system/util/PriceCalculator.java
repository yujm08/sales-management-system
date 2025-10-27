// PriceCalculator.java - 가격 계산 유틸리티
package com.mynet.sales_management_system.util;

import com.mynet.sales_management_system.entity.ProductPriceHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 가격 계산 관련 유틸리티
 * - 매출 금액 계산
 * - 이익 계산
 * - 달성률 계산
 */
@Component
public class PriceCalculator {
    
    /**
     * 매출 금액 계산 (수량 × 공급가)
     */
    public static BigDecimal calculateRevenue(int quantity, BigDecimal supplyPrice) {
        return BigDecimal.valueOf(quantity).multiply(supplyPrice);
    }
    
    /**
     * 이익 계산 (매출 - 원가)
     */
    public static BigDecimal calculateProfit(int quantity, BigDecimal supplyPrice, BigDecimal costPrice) {
        BigDecimal revenue = calculateRevenue(quantity, supplyPrice);
        BigDecimal totalCost = BigDecimal.valueOf(quantity).multiply(costPrice);
        return revenue.subtract(totalCost);
    }
    
    /**
     * 이익률 계산 (이익 / 매출 × 100)
     */
    public static BigDecimal calculateProfitRate(BigDecimal profit, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return profit.divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 달성률 계산 (실적 / 목표 × 100)
     */
    public static BigDecimal calculateAchievementRate(int actual, int target) {
        if (target == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(actual)
                .divide(BigDecimal.valueOf(target), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 제품 가격 정보로부터 매출/이익 계산
     */
    public static SalesAmount calculateSalesAmount(int quantity, ProductPriceHistory priceHistory) {
        if (priceHistory == null) {
            return SalesAmount.builder()
                .revenue(BigDecimal.ZERO)
                .profit(BigDecimal.ZERO)
                .build();
        }
        
        BigDecimal revenue = calculateRevenue(quantity, priceHistory.getSupplyPrice());
        BigDecimal profit = calculateProfit(quantity, priceHistory.getSupplyPrice(), priceHistory.getCostPrice());
        
        return SalesAmount.builder()
            .revenue(revenue)
            .profit(profit)
            .build();
    }
    
    /**
     * 매출 금액 DTO
     */
    public static class SalesAmount {
        private final BigDecimal revenue;
        private final BigDecimal profit;
        
        private SalesAmount(BigDecimal revenue, BigDecimal profit) {
            this.revenue = revenue;
            this.profit = profit;
        }
        
        public static SalesAmountBuilder builder() {
            return new SalesAmountBuilder();
        }
        
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getProfit() { return profit; }
        
        public static class SalesAmountBuilder {
            private BigDecimal revenue;
            private BigDecimal profit;
            
            public SalesAmountBuilder revenue(BigDecimal revenue) {
                this.revenue = revenue;
                return this;
            }
            
            public SalesAmountBuilder profit(BigDecimal profit) {
                this.profit = profit;
                return this;
            }
            
            public SalesAmount build() {
                return new SalesAmount(revenue, profit);
            }
        }
    }
}