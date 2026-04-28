package com.nuonuo.bootstrap.config;

import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web 上下文拦截与受控异步线程池配置。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new ContextCleanupInterceptor()).addPathPatterns("/**");
    }

    /**
     * 旁路异步治理线程池，严禁使用无界队列并强制清理线程上下文。
     *
     * @return 受控异步执行器
     */
    @Bean(name = "asyncGovernanceExecutor")
    public Executor asyncGovernanceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("nuonuo-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(contextPropagatingDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator contextPropagatingDecorator() {
        return runnable -> {
            String userId = UserContext.getUserId();
            String traceId = MDC.get(Result.TRACE_ID_KEY);
            return () -> {
                try {
                    UserContext.setUserId(userId);
                    if (traceId != null) {
                        MDC.put(Result.TRACE_ID_KEY, traceId);
                    }
                    runnable.run();
                } finally {
                    UserContext.clear();
                    MDC.clear();
                }
            };
        };
    }

    /**
     * 请求上下文拦截器，在请求完成后强制清理 ThreadLocal 与 MDC。
     */
    private static final class ContextCleanupInterceptor implements HandlerInterceptor {

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

            String userId = request.getHeader(HEADER_USER_ID);
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
}
