package com.nuonuo.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionContextServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ListOperations<String, String> listOperations;

    private SessionContextServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        service = new SessionContextServiceImpl(redisTemplate, new ObjectMapper());
    }

    @Test
    void buildShortTermContext_shouldIncludeSummaryAndRecentTurns() {
        when(valueOperations.get("nuonuo:session:s1:summary")).thenReturn("用户是干敏肌，关注抗老。");
        when(listOperations.range("nuonuo:session:s1:recent", -6, -1))
                .thenReturn(List.of("{\"role\":\"user\",\"content\":\"早C晚A合适吗\"}",
                        "{\"role\":\"assistant\",\"content\":\"建议温和建立耐受\"}"));

        String ctx = service.buildShortTermContext("s1");
        assertTrue(ctx.contains("用户是干敏肌，关注抗老。"));
        assertTrue(ctx.contains("早C晚A合适吗"));
        assertTrue(ctx.contains("建议温和建立耐受"));
    }

    @Test
    void appendAndCompress_shouldCompressOlderTurns_whenTurnsExceed15() {
        List<String> existing = new ArrayList<>();
        for (int i = 1; i <= 32; i++) {
            existing.add("{\"role\":\"" + (i % 2 == 1 ? "user" : "assistant") + "\",\"content\":\"第" + i + "条消息\"}");
        }
        when(listOperations.range("nuonuo:session:s2:recent", 0, -1)).thenReturn(existing);

        service.appendAndCompress("s2", "新问题", "新回答");

        verify(valueOperations).set(eq("nuonuo:session:s2:summary"), anyString(), any());
    }
}
