package com.nuonuo.common.exception;

/**
 * 统一业务错误码枚举。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public enum ErrorCode {

    /**
     * 操作成功。
     */
    SUCCESS("00000", "success"),

    /**
     * 请求参数校验失败。
     */
    PARAM_INVALID("A0400", "请求参数校验失败"),

    /**
     * 用户未登录或认证凭证失效。
     */
    UNAUTHORIZED("A0401", "用户未认证或登录态已失效"),

    /**
     * 无权访问目标资源。
     */
    FORBIDDEN("A0403", "无权访问该资源"),

    /**
     * 目标资源不存在。
     */
    RESOURCE_NOT_FOUND("A0404", "目标资源不存在"),

    /**
     * 数据状态冲突或并发版本已变更。
     */
    STATE_CONFLICT("A0409", "数据状态冲突，请刷新后重试"),

    /**
     * 请求频率超出限制。
     */
    RATE_LIMITED("A0429", "请求过于频繁，请稍后再试"),

    /**
     * 触发成分配伍禁忌硬性拦截。
     */
    INGREDIENT_CONFLICT_BLOCKED("B0100", "检测到成分配伍禁忌风险"),

    /**
     * 系统内部业务异常。
     */
    INTERNAL_ERROR("B0500", "系统内部异常，请稍后再试"),

    /**
     * 外部大模型服务暂时不可用。
     */
    AI_MODEL_UNAVAILABLE("C0503", "智能护肤助手暂时繁忙，请稍后再试"),

    /**
     * 对象存储服务操作失败。
     */
    OSS_OPERATION_FAILED("C0504", "多媒体存储服务异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
