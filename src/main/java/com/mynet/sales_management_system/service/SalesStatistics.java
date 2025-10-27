// SalesStatistics.java - 매출 통계 DTO
package com.mynet.sales_management_system.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesStatistics {
    private int totalQuantity;      // 총 수량
    private BigDecimal totalRevenue; // 총 매출
    private BigDecimal totalProfit;  // 총 이익
}