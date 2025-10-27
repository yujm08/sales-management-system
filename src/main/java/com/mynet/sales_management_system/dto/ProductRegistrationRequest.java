package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRegistrationRequest {
    private String category;
    private String productName;
    private BigDecimal costPrice;
    private BigDecimal supplyPrice;
}