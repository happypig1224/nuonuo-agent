package com.nuonuo.common.exception;

import java.util.Objects;

/**
 * 统一业务异常类，承载确定性的错误码与业务上下文信息。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * 使用默认错误码文案构造业务异常。
     *
     * @param errorCode 错误码枚举，不可为 null
     */
    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义描述构造业务异常。
     *
     * @param errorCode 错误码枚举，不可为 null
     * @param message   面向调用方的明确错误描述
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message != null ? message : Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * 保留原始根因异常构造业务异常。
     *
     * @param errorCode 错误码枚举，不可为 null
     * @param message   面向调用方的明确错误描述
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
