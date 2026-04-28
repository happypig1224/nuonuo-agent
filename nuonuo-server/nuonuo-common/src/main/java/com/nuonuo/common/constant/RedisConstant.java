package com.nuonuo.common.constant;

import java.time.Duration;

/**
 * 全局 Redis Key 前缀与 TTL 常量字典。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public final class RedisConstant {

    /**
     * 用户会话 Token 缓存 Key 前缀。
     */
    public static final String AUTH_TOKEN_KEY_PREFIX = "nuonuo:auth:token:";

    /**
     * 用户会话 Token 默认有效期（7 天）。
     */
    public static final Duration AUTH_TOKEN_TTL = Duration.ofDays(7);

    /**
     * 注册验证码缓存 Key 前缀。
     */
    public static final String AUTH_OTP_KEY_PREFIX = "nuonuo:auth:otp:";

    /**
     * 注册验证码默认有效期（5 分钟）。
     */
    public static final Duration AUTH_OTP_TTL = Duration.ofMinutes(5);

    private RedisConstant() {
    }
}
