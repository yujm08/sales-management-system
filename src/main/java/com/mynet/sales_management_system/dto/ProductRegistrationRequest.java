package com.mynet.sales_management_system.dto;

import lombok.Data;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class ProductRegistrationRequest {
    @NotBlank(message = "카테고리는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣\\s\\-_]+$", message = "카테고리명에 허용되지 않는 문자가 포함되어 있습니다")
    private String category;

    @NotBlank(message = "제품명은 필수입니다")
    @Size(max = 200, message = "제품명은 200자를 초과할 수 없습니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣\\s\\-_()./]+$", message = "제품명에 허용되지 않는 문자가 포함되어 있습니다")
    private String productName;

    @NotNull(message = "원가는 필수입니다")
    @DecimalMin(value = "0.0", message = "원가는 0 이상이어야 합니다")
    private BigDecimal costPrice;

    @NotNull(message = "공급가는 필수입니다")
    @DecimalMin(value = "0.0", message = "공급가는 0 이상이어야 합니다")
    private BigDecimal supplyPrice;
}