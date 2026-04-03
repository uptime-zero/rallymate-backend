package com.rallymate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendSmsCodeRequest {
    
    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(
            regexp = "^(010|011|016|017|018|019)\\d{8}$",
            message = "휴대폰 번호 형식이 올바르지 않습니다. 예) 01012345678"
    )
    private String phoneNumber;
}

