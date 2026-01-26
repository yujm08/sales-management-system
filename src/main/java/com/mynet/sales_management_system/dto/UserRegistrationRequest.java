package com.mynet.sales_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 20, message = "사용자명은 4-20자여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$", message = "사용자명은 영문, 숫자, 한글, 언더스코어만 가능합니다 (특수문자 금지)")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 4, max = 100, message = "비밀번호는 4-100자여야 합니다")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;

    @NotNull(message = "회사는 필수입니다")
    @Min(value = 1, message = "올바른 회사를 선택해주세요")
    private Long companyId;
}