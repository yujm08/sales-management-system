// ComparisonData.java - 비교 데이터 DTO
package com.mynet.sales_management_system.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComparisonData {
    private BigDecimal currentProfit;   // 현재 이익
    private BigDecimal previousProfit;  // 이전 이익
    private BigDecimal profitChange;    // 이익 변화량
    private boolean isIncrease;         // 증가 여부 (화살표 방향 결정용)
}
