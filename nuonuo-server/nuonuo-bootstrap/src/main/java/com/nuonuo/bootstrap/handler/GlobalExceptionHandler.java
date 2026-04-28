package com.nuonuo.bootstrap.handler;

import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;
import com.nuonuo.common.model.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局统一异常处理器，确保对外响应语义明确且不泄漏内部堆栈。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求体参数校验失败异常。
     *
     * @param ex 参数校验异常
     * @return 400 响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : ErrorCode.PARAM_INVALID.getMessage();
        log.warn("Request body validation failed, userId={}, message={}", UserContext.getUserId(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 处理路径或查询参数约束校验失败异常。
     *
     * @param ex 约束校验异常
     * @return 400 响应体
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("Request parameter constraint violated, userId={}, message={}", UserContext.getUserId(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 处理请求体 JSON 解析失败或不可读异常。
     *
     * @param ex 请求体解析异常
     * @return 400 响应体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body not readable, userId={}, message={}", UserContext.getUserId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), "请求体格式错误或不可读"));
    }

    /**
     * 处理缺少必填查询参数异常。
     *
     * @param ex 缺少参数异常
     * @return 400 响应体
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {
        String message = "缺少必填参数: " + ex.getParameterName();
        log.warn("Missing required request parameter, userId={}, parameter={}",
                UserContext.getUserId(), ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 处理确定性业务异常。
     *
     * @param ex 业务异常
     * @return 对应 HTTP 状态码与业务错误码响应体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = resolveHttpStatus(errorCode);
        log.warn("Business exception handled, code={}, userId={}, message={}",
                errorCode.getCode(), UserContext.getUserId(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(Result.fail(errorCode.getCode(), ex.getMessage()));
    }

    /**
     * 兜底处理未预期系统异常，屏蔽底层技术细节。
     *
     * @param ex 未捕获异常
     * @return 500 脱敏响应体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected system exception, userId={}", UserContext.getUserId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case PARAM_INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case STATE_CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case AI_MODEL_UNAVAILABLE, OSS_OPERATION_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
