package com.rallymate.auth.service;

import com.rallymate.auth.dto.TokenResponse;
import com.rallymate.auth.entity.RefreshToken;
import com.rallymate.auth.repository.RefreshTokenRepository;
import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.jwt.JwtProvider;
import com.rallymate.user.entity.User;
import com.rallymate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * RTR 방식으로 Access Token 및 Refresh Token 재발급
     * @param refreshToken 기존 Refresh Token
     * @return 새로운 Access Token 및 Refresh Token
     */
    @Transactional
    public TokenResponse reissueTokens(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        }

        // 2. DB에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage()));

        // 3. Revoked 여부 확인
        if (storedToken.isRevoked()) {
            throw new RuntimeException(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        }

        // 4. 만료 여부 확인
        if (storedToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(ErrorCode.EXPIRED_TOKEN.getMessage());
        }

        // 5. UID 추출 및 사용자 조회
        String uid = jwtProvider.extractSubject(refreshToken);
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getMessage()));

        // 6. 기존 Refresh Token revoke 처리
        storedToken.revoke();

        // 7. 새로운 Access Token 및 Refresh Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(uid);
        String newRefreshToken = jwtProvider.generateRefreshToken(uid);

        // 8. 새로운 Refresh Token DB 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(14); // 14일 후 만료

        RefreshToken newToken = RefreshToken.of(
                uid,
                newRefreshToken,
                false,
                expiredAt,
                now
        );
        refreshTokenRepository.save(newToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그인 시 Access Token 및 Refresh Token 발급
     * @param uid 사용자 UID
     * @return Access Token 및 Refresh Token
     */
    @Transactional
    public TokenResponse issueTokens(String uid) {
        // 1. 사용자 조회
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getMessage()));

        // 2. 기존 Refresh Token 삭제 (재로그인 시)
        refreshTokenRepository.deleteByUid(uid);

        // 3. 새로운 Access Token 및 Refresh Token 발급
        String accessToken = jwtProvider.generateAccessToken(uid);
        String refreshToken = jwtProvider.generateRefreshToken(uid);

        // 4. Refresh Token DB 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(14);

        RefreshToken newToken = RefreshToken.of(
                uid,
                refreshToken,
                false,
                expiredAt,
                now
        );
        refreshTokenRepository.save(newToken);

        return new TokenResponse(accessToken, refreshToken);
    }
}
