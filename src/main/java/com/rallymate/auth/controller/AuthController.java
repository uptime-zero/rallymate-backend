package com.rallymate.auth.controller;

import com.rallymate.auth.dto.TokenResponse;
import com.rallymate.auth.service.AuthService;
import com.rallymate.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
     * Access Token 및 Refresh Token 재발급 (RTR 방식)
     * @param request HTTP 요청 (Header에서 Refresh Token 추출)
     * @return ApiResponse<TokenResponse> 및 Header에 토큰 포함
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissueTokens(HttpServletRequest request) {
        // 1. Header에서 Refresh Token 추출
        String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(
                            com.rallymate.global.exception.ErrorCode.REFRESH_TOKEN_NOT_FOUND
                    ));
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
