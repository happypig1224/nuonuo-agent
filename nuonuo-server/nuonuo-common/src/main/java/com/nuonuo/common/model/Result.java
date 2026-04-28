package com.nuonuo.common.model;

import com.nuonuo.common.exception.ErrorCode;
import org.slf4j.MDC;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一 HTTP 接口响应骨架。
 *
 * @param <T> 响应业务载荷类型
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TRACE_ID_KEY = "traceId";

    private String code;
    private String message;
    private T data;
    private String traceId;
    private Long timestamp;

    public Result() {
    }

    public Result(String code, String message, T data, String traceId, Long timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

    /**
     * 构造无载荷的成功响应。
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构造携带业务数据的成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                MDC.get(TRACE_ID_KEY),
                System.currentTimeMillis()
        );
    }

    /**
     * 基于标准错误码构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 基于自定义错误信息构造失败响应。
     *
     * @param code    错误码字符串
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(
                code,
                message,
                null,
                MDC.get(TRACE_ID_KEY),
                System.currentTimeMillis()
        );
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Result<?> result = (Result<?>) o;
        return Objects.equals(code, result.code)
                && Objects.equals(message, result.message)
                && Objects.equals(data, result.data)
                && Objects.equals(traceId, result.traceId)
                && Objects.equals(timestamp, result.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data, traceId, timestamp);
    }

    @Override
    public String toString() {
        return "Result{"
                + "code='" + code + '\''
                + ", message='" + message + '\''
                + ", data=" + data
                + ", traceId='" + traceId + '\''
                + ", timestamp=" + timestamp
                + '}';
    }
}
