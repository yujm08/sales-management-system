package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductInputDTO {
    private Long id;
    private String category;
    private String productCode;
    private String productName;
    private BigDecimal supplyPrice;
    private Integer quantity;
    private String categoryClass; // CSS 클래스명

    public ProductInputDTO() {
        this.quantity = 0; // 기본값
    }
}