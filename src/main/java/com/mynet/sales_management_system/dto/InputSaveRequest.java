package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.util.Map;

@Data
public class InputSaveRequest {
    private String date;
    private Map<Long, Integer> quantities;
}