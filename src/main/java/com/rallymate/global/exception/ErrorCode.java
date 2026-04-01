package com.rallymate.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth (1xxx)
    INVALID_SMS_CODE(1001, HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    EXPIRED_SMS_CODE(1002, HttpStatus.BAD_REQUEST, "만료된 인증 코드입니다."),
    UNAUTHORIZED(1003, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // User (2xxx)
    USER_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),

    // Matching (3xxx)
    ALREADY_MATCHING(3001, HttpStatus.BAD_REQUEST, "이미 매칭 중입니다."),
    MATCHING_SESSION_NOT_FOUND(3002, HttpStatus.NOT_FOUND, "매칭 세션을 찾을 수 없습니다."),
    MATCHING_UNAVAILABLE(3003, HttpStatus.FORBIDDEN, "패널티로 인해 매칭이 불가합니다."),

    // Common (8xxx)
    INVALID_REQUEST(8001, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_FAILED(8002, HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Server (9xxx)
    INTERNAL_SERVER_ERROR(9001, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
