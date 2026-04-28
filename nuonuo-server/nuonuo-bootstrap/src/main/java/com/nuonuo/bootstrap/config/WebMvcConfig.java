package com.nuonuo.bootstrap.config;

import com.nuonuo.bootstrap.interceptor.AuthInterceptor;
import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web 拦截器注册与受控异步线程池配置。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/user/login",
                        "/api/v1/user/register"
                );
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
}
