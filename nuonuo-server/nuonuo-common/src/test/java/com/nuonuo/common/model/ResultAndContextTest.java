package com.nuonuo.common.model;

import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Result、UserContext 与 BusinessException 单元测试。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
class ResultAndContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void shouldBuildSuccessResultWithTraceId() {
        MDC.put(Result.TRACE_ID_KEY, "trace-123");

        Result<String> result = Result.success("ok");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("ok", result.getData());
        assertEquals("trace-123", result.getTraceId());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldBuildFailResultFromErrorCode() {
        Result<Void> result = Result.fail(ErrorCode.INGREDIENT_CONFLICT_BLOCKED);

        assertEquals("B0100", result.getCode());
        assertEquals(ErrorCode.INGREDIENT_CONFLICT_BLOCKED.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void shouldManageAndClearUserContext() {
        assertNull(UserContext.getUserId());
        assertThrows(BusinessException.class, UserContext::getRequiredUserId);

        UserContext.setUserId("  user_001  ");
        assertEquals("user_001", UserContext.getRequiredUserId());

        UserContext.clear();
        assertNull(UserContext.getUserId());
    }
}
