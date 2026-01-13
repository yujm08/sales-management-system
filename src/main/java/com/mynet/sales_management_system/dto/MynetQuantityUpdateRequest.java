package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.util.Map;

/**
 * 마이넷 수량 수정용 DTO
 */
@Data
public class MynetQuantityUpdateRequest {
    private String companyId;
    private String salesDate;
    private Map<Long, Integer> quantities;
}