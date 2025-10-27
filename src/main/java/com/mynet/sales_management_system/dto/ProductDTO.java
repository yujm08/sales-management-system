package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String productCode;
    private String productName;
    private String category;
    private boolean active;
    private LocalDateTime createdAt;
    private BigDecimal currentCostPrice;
    private BigDecimal currentSupplyPrice;
    private String categoryClass;
}