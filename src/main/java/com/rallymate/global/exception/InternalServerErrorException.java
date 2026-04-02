package com.rallymate.global.exception;

public class InternalServerErrorException extends BaseException {
    public InternalServerErrorException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerErrorException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
