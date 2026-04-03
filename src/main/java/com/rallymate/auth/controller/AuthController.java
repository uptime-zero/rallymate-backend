package com.rallymate.auth.controller;

import com.rallymate.auth.dto.LoginRequest;
import com.rallymate.auth.dto.SendSmsCodeRequest;
import com.rallymate.auth.dto.SignUpRequest;
import com.rallymate.auth.dto.TokenResponse;
import com.rallymate.auth.service.AuthService;
import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    /**
     * SMS 인증코드를 발급합니다.
     * <p>입력된 휴대폰 번호에 대해 6자리 인증 코드를 생성하고 Redis에 저장합니다.</p>
     * <p>인증 코드는 <b>회원가입</b> 또는 <b>로그인</b> 성공 시 1회용으로 삭제됩니다.</p>
     *
     * @param request 인증 코드 발급 요청 (휴대폰 번호)
     * @return {@code ResponseEntity<ApiResponse<Void>>} 성공 시 200 OK
     */
    @PostMapping("/sms/codes")
    public ResponseEntity<ApiResponse<Void>> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        authService.sendSmsCode(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 회원가입을 처리합니다.
     * <p>휴대폰 번호와 SMS 인증 코드를 검증한 후 새로운 사용자 계정을 생성합니다. 성공 시 별도의 토큰 발급 없이 성공 상태만을 반환합니다.</p>
     *
     * @param request 가입 정보 (휴대폰 번호, 인증 코드, 닉네임 등)
     * @return {@code ResponseEntity<ApiResponse<Void>>} 성공 시 200 OK
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 로그인을 수행하고 인증 토큰을 발급합니다.
     * <p>인증 코드 검증 후, 발급된 Access Token과 Refresh Token은 응답 헤더와 본문에 모두 포함됩니다.</p>
     *
     * @param request 로그인 정보 (휴대폰 번호, 인증 코드)
     * @return {@code ResponseEntity<ApiResponse<TokenResponse>>} 발급된 토큰 정보와 함께 200 OK 반환
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokenResponse.getAccessToken());
        headers.set(REFRESH_TOKEN_HEADER, tokenResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(ApiResponse.ok(tokenResponse));
    }

    /**
     * Refresh Token을 사용하여 토큰 세트를 재발급합니다. (RTR 방식)
     * <p>요청 헤더의 Refresh Token 유효성을 확인하고, 새로운 토큰 세트를 생성하여 헤더와 본문에 담아 반환합니다.</p>
     *
     * @param request HTTP 요청 객체 (헤더에서 {@code X-Refresh-Token} 추출 용도)
     * @return {@code ResponseEntity<ApiResponse<TokenResponse>>} 갱신된 토큰 정보
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissueTokens(HttpServletRequest request) {
        // 1. Header에서 Refresh Token 추출
        String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        }

        // 2. AuthService를 통해 토큰 재발급
        TokenResponse tokenResponse = authService.reissueTokens(refreshToken);

        // 3. Response Header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokenResponse.getAccessToken());
        headers.set(REFRESH_TOKEN_HEADER, tokenResponse.getRefreshToken());

        // 4. Response Body 및 Header 반환
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(ApiResponse.ok(tokenResponse));
    }
}
