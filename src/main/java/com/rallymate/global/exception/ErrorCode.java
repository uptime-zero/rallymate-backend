package com.rallymate.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth (2xxx)
    INVALID_SMS_CODE(2001, HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    EXPIRED_SMS_CODE(2002, HttpStatus.BAD_REQUEST, "만료된 인증 코드입니다."),
    UNAUTHORIZED(2003, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(2004, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(2005, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(2006, HttpStatus.UNAUTHORIZED, "토큰이 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(2007, HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(2008, HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),

    // User (3xxx)
    USER_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    ALREADY_REGISTERED(3002, HttpStatus.BAD_REQUEST, "이미 등록된 유저입니다."),

    // Matching (4xxx)
    ALREADY_MATCHING(4001, HttpStatus.BAD_REQUEST, "이미 매칭 중입니다."),
    MATCHING_SESSION_NOT_FOUND(4002, HttpStatus.NOT_FOUND, "매칭 세션을 찾을 수 없습니다."),
    MATCHING_UNAVAILABLE(4003, HttpStatus.FORBIDDEN, "패널티로 인해 매칭이 불가합니다."),
    NOT_A_MATCH_PARTICIPANT(4004, HttpStatus.FORBIDDEN, "매칭 세션 참여자가 아닙니다."),

    // Common (8xxx)
    INVALID_REQUEST(8001, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_FAILED(8002, HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Server (9xxx)
    INTERNAL_SERVER_ERROR(9001, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
