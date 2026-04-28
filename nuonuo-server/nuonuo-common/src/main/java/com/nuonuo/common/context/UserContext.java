package com.nuonuo.common.context;

import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;

/**
 * 服务端用户身份单点上下文事实源。
 * 请求与异步任务结束时必须调用 {@link #clear()} 释放线程变量，防止复用串号。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public final class UserContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 绑定当前线程的用户业务 ID。
     *
     * @param userId 用户业务 ID
     */
    public static void setUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            CURRENT_USER_ID.remove();
            return;
        }
        CURRENT_USER_ID.set(userId.trim());
    }

    /**
     * 获取当前线程绑定的用户业务 ID，未绑定时返回 null。
     *
     * @return 用户业务 ID 或 null
     */
    public static String getUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前线程绑定的用户业务 ID，缺失时立即抛出未认证异常。
     *
     * @return 用户业务 ID
     */
    public static String getRequiredUserId() {
        String userId = CURRENT_USER_ID.get();
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 清理当前线程上下文，必须在请求结束或异步任务 finally 块中调用。
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
