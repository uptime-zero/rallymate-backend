package com.rallymate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    private String phoneNumber;

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String smsCode;
}

