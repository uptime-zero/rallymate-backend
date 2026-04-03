package com.rallymate.auth.dto;

import com.rallymate.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    private String phoneNumber;

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String smsCode;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    @NotBlank(message = "프로필 URL은 필수입니다.")
    private String profileUrl;

    @NotBlank(message = "선호 스포츠는 필수입니다.")
    private String preferredSport;

    @NotBlank(message = "활동 지역은 필수입니다.")
    private String activityArea;
}

