package com.nuonuo.bootstrap.interceptor;

import com.nuonuo.common.constant.RedisConstant;
import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;
import com.nuonuo.common.model.Result;
import com.nuonuo.common.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 全局认证与请求上下文拦截器。
 * 负责校验 Bearer Token、绑定 {@link UserContext} 与链路 {@code traceId}，并在请求结束后强制清理线程变量。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(Result.TRACE_ID_KEY, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);

        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再访问");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭据无效");
        }
        String userId = RedisUtils.get(RedisConstant.AUTH_TOKEN_KEY_PREFIX + token);
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已过期，请重新登录");
        }
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        UserContext.clear();
        MDC.clear();
    }
}
