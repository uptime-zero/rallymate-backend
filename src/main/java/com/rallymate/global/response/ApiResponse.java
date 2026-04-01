package com.rallymate.global.response;

import com.rallymate.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final int code;
    private final String msg;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(1000, "요청 성공", data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String msg) {
        return new ApiResponse<>(errorCode.getCode(), msg, null);
    }
}
