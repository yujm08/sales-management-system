package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.util.Map;

/**
 * 마이넷 목표 수정용 DTO
 */
@Data
public class MynetTargetUpdateRequest {
    private String companyId;
    private String targetDate;
    private Map<Long, Integer> targetQuantities;
}