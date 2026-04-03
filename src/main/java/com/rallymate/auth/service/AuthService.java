package com.rallymate.auth.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.rallymate.auth.dto.LoginRequest;
import com.rallymate.auth.dto.SendSmsCodeRequest;
import com.rallymate.auth.dto.SignUpRequest;
import com.rallymate.auth.dto.TokenResponse;
import com.rallymate.auth.entity.RefreshToken;
import com.rallymate.auth.repository.RefreshTokenRepository;
import com.rallymate.auth.repository.SmsRepository;
import com.rallymate.global.crypto.PhoneCrypto;
import com.rallymate.global.exception.BadRequestException;
import com.rallymate.global.exception.NotFoundException;
import com.rallymate.global.exception.UnauthorizedException;
import com.rallymate.global.jwt.JwtProvider;
import com.rallymate.user.entity.User;
import com.rallymate.user.entity.UserRole;
import com.rallymate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static com.rallymate.global.exception.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SmsRepository smsRepository;
    private final PhoneCrypto phoneCrypto;

    /**
     * SMS 인증 코드를 발급합니다.
     * <p>입력된 {@code phoneNumber}에 대해 6자리 인증 코드를 생성하여 Redis에 저장합니다.</p>
     * <p>인증 성공 시(회원가입/로그인) 해당 코드는 1회용으로 삭제됩니다.</p>
     *
     * @param request 인증 코드 발급 요청 (휴대폰 번호)
     */
    @Transactional
    public void sendSmsCode(SendSmsCodeRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();

        SecureRandom secureRandom = new SecureRandom();
        int code = secureRandom.nextInt(1_000_000); // 0 ~ 999999
        String verifyCode = String.format("%06d", code); // 항상 6자리

        // Redis 저장 (기존 코드가 있으면 덮어씀)
        smsRepository.save(phoneNumber, verifyCode);

        // 발급 성공 로그(요청에서 코드 값도 남기도록 지정)
        log.info("Generated SMS verification code. phoneNumber={}, verifyCode={}", phoneNumber, verifyCode);
    }

    /**
     * 회원가입을 처리합니다.
     * <p>휴대폰 번호와 SMS 인증 코드를 검증한 후, 중복 가입 여부를 확인하여 새 사용자를 생성합니다.</p>
     *
     * @param request 가입에 필요한 사용자 정보 (휴대폰 번호, 인증 코드, 닉네임 등)
     * @throws BadRequestException 인증 코드가 만료되었거나 일치하지 않는 경우, 또는 이미 가입된 휴대폰 번호인 경우
     */
    @Transactional
    public void signUp(SignUpRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        String smsCode = request.getSmsCode().trim();

        String storedCode = smsRepository.find(phoneNumber)
                .orElseThrow(() -> new BadRequestException(EXPIRED_SMS_CODE));

        if (!storedCode.equals(smsCode)) {
            throw new BadRequestException(INVALID_SMS_CODE);
        }

        String phoneHash = phoneCrypto.hash(phoneNumber);

        if (userRepository.existsByPhoneHash(phoneHash)) {
            throw new BadRequestException(ALREADY_REGISTERED);
        }

        String phoneEnc = phoneCrypto.encrypt(phoneNumber);
        String uid = UlidCreator.getUlid().toString(); // User.uid 길이(26)와 동일

        User user = User.of(
                uid,
                phoneEnc,
                phoneHash,
                request.getNickname(),
                request.getGender(),
                request.getProfileUrl(),
                request.getPreferredSport(),
                request.getActivityArea(),
                UserRole.USER
        );

        userRepository.save(user);

        // 인증 성공 후 1회용 코드 삭제 (서버 로그로 남김)
        log.info("Deleting SMS verification code after successful signup. phoneNumber={}", phoneNumber);
        smsRepository.delete(phoneNumber);
    }

    /**
     * 로그인을 처리하고 토큰을 발급합니다.
     * <p>휴대폰 번호와 SMS 인증 코드를 검증한 후, 등록된 사용자라면 액세스 및 리프레시 토큰을 반환합니다.</p>
     *
     * @param request 로그인 정보 (휴대폰 번호, 인증 코드)
     * @return {@link TokenResponse} 발급된 Access Token 및 Refresh Token
     * @throws BadRequestException 인증 코드가 만료되었거나 일치하지 않는 경우
     * @throws NotFoundException   해당 휴대폰 번호로 가입된 사용자가 없는 경우
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        String smsCode = request.getSmsCode().trim();

        String storedCode = smsRepository.find(phoneNumber)
                .orElseThrow(() -> new BadRequestException(EXPIRED_SMS_CODE));

        if (!storedCode.equals(smsCode)) {
            throw new BadRequestException(INVALID_SMS_CODE);
        }

        String phoneHash = phoneCrypto.hash(phoneNumber);

        User user = userRepository.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));

        // 인증 성공 후 1회용 코드 삭제 (서버 로그로 남김)
        log.info("Deleting SMS verification code after successful login. phoneNumber={}", phoneNumber);
        smsRepository.delete(phoneNumber);

        return issueTokens(user.getUid());
    }

    /**
     * RTR(Refresh Token Rotation) 방식을 이용하여 토큰 세트를 재발급합니다.
     * <p>기존 리프레시 토큰의 유효성을 검증하고, 사용된 토큰은 폐기(Revoke)한 뒤 새로운 토큰들을 생성합니다.</p>
     *
     * @param refreshToken 클라이언트로부터 전달받은 기존 Refresh Token
     * @return {@link TokenResponse} 새롭게 발급된 Access Token 및 Refresh Token
     * @throws UnauthorizedException 토큰이 유효하지 않거나, 이미 폐기되었거나, 만료된 경우
     * @throws NotFoundException     토큰에 해당하는 사용자가 존재하지 않는 경우
     */
    @Transactional
    public TokenResponse reissueTokens(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN, INVALID_REFRESH_TOKEN.getMessage());
        }

        // 2. DB에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(REFRESH_TOKEN_NOT_FOUND, REFRESH_TOKEN_NOT_FOUND.getMessage()));

        // 3. Revoked 여부 확인
        if (storedToken.isRevoked()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN, INVALID_REFRESH_TOKEN.getMessage());
        }

        // 4. 만료 여부 확인
        if (storedToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException(EXPIRED_TOKEN, EXPIRED_TOKEN.getMessage());
        }

        // 5. UID 추출 및 사용자 조회
        String uid = jwtProvider.extractSubject(refreshToken);
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));

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

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    /**
     * 사용자 식별자(UID)를 기반으로 새로운 토큰 세트를 발급하고 저장합니다.
     * <p>기존에 해당 사용자가 보유했던 리프레시 토큰은 모두 삭제하여 중복 로그인을 방지합니다.</p>
     *
     * @param uid 사용자의 고유 식별자 (ULID)
     * @return {@link TokenResponse} 생성된 Access Token 및 Refresh Token
     * @throws NotFoundException 존재하지 않는 UID인 경우
     */
    @Transactional
    public TokenResponse issueTokens(String uid) {
        // 1. 사용자 조회
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));

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

        return TokenResponse.of(accessToken, refreshToken);
    }
}
