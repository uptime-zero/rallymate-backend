package com.rallymate.global.handler;

import com.rallymate.global.exception.*;
import com.rallymate.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 공통 처리 (부모로 한 번에 처리)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        // 5xx는 ERROR, 4xx는 WARN으로 레벨 구분
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("ServerError: code={}, msg={}", errorCode.getCode(), errorCode.getMessage());
        } else {
            log.warn("ClientError: code={}, msg={}", errorCode.getCode(), errorCode.getMessage());
        }
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    // @Valid 검증 실패 (RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.VALIDATION_FAILED.getMessage());
        log.warn("Validation failed: {}", msg);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, msg));
    }

    // @Valid 검증 실패 (RequestParam, PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ErrorCode.VALIDATION_FAILED.getMessage());
        log.warn("ConstraintViolation: {}", msg);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, msg));
    }

    // 모든 예외 최종 핸들러 (처리되지 않은 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
