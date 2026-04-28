package com.nuonuo.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.dto.ChatStreamRequest;
import com.nuonuo.chat.mapper.ChatMessageMapper;
import com.nuonuo.common.utils.LlmClient;
import com.nuonuo.ingredient.dto.IngredientCheckResponse;
import com.nuonuo.ingredient.service.IngredientCheckService;
import com.nuonuo.media.service.MediaAssetService;
import com.nuonuo.memory.dto.MemoryItemResponse;
import com.nuonuo.memory.service.MemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private MediaAssetService mediaAssetService;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private MemoryService memoryService;
    @Mock
    private IngredientCheckService ingredientCheckService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                llmClient,
                mediaAssetService,
                chatMessageMapper,
                new ObjectMapper(),
                memoryService,
                ingredientCheckService,
                null
        );
    }

    @Test
    void streamChat_shouldInjectMemoryAndGuardNotice_whenConflictDetected() throws Exception {
        ChatStreamRequest request = new ChatStreamRequest();
        request.setSessionId("sess-tdd-1");
        request.setPrompt("早C晚A可以和维A醇一起用吗");

        MemoryItemResponse memory = new MemoryItemResponse(
                "mem-1", "SKIN_PROFILE", "SKIN_TYPE", "干敏肌", BigDecimal.valueOf(0.95), LocalDateTime.now()
        );
        when(memoryService.listActiveMemories("u-1")).thenReturn(List.of(memory));

        IngredientCheckResponse.ConflictDetail conflict = new IngredientCheckResponse.ConflictDetail(
                "SINGLE", "HIGH", "维A醇 + 维C", "强刺激叠加", "建议早晚分开使用"
        );
        IngredientCheckResponse checkResponse = new IngredientCheckResponse(
                List.of("维A醇", "维C"),
                List.of(conflict),
                true,
                "L3_GUARD_NOTICE: 严禁叠加"
        );
        when(ingredientCheckService.check("u-1", "sess-tdd-1", "早C晚A可以和维A醇一起用吗"))
                .thenReturn(checkResponse);

        when(llmClient.isConfigured()).thenReturn(true);
        doAnswer(invocation -> {
            Consumer<String> onChunk = invocation.getArgument(5);
            onChunk.accept("建议分开使用哦。");
            return null;
        }).when(llmClient).streamChat(anyString(), anyString(), any(), any(), any(), any());

        SseEmitter emitter = chatService.streamChat("u-1", request);
        assertNotNull(emitter);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, timeout(2000)).streamChat(
                eq("chat.system"),
                promptCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains("干敏肌"), "Prompt should contain active user memory");
        assertTrue(capturedPrompt.contains("L3_GUARD_NOTICE"), "Prompt should contain L3 guard notice");
    }

    @Test
    void streamChat_shouldInvokeExtractAndUpsert_whenUserSharesProfile() throws Exception {
        ChatStreamRequest request = new ChatStreamRequest();
        request.setSessionId("sess-tdd-mem");
        request.setPrompt("我是干敏肌，平时想抗老");

        when(memoryService.listActiveMemories("u-2")).thenReturn(List.of());
        when(ingredientCheckService.check("u-2", "sess-tdd-mem", "我是干敏肌，平时想抗老")).thenReturn(null);
        MemoryItemResponse extracted = new MemoryItemResponse(
                "mem-2", "SKIN_PROFILE", "SKIN_TYPE", "干敏肌", BigDecimal.valueOf(0.95), LocalDateTime.now()
        );
        when(memoryService.extractAndUpsert("u-2", "我是干敏肌，平时想抗老")).thenReturn(List.of(extracted));
        when(llmClient.isConfigured()).thenReturn(false);

        SseEmitter emitter = chatService.streamChat("u-2", request);
        assertNotNull(emitter);

        verify(memoryService, timeout(2000)).extractAndUpsert("u-2", "我是干敏肌，平时想抗老");
    }

    @Test
    void streamChat_shouldCompleteCleanly_whenRoutineRequested() {
        ChatStreamRequest request = new ChatStreamRequest();
        request.setSessionId("sess-tdd-routine");
        request.setPrompt("帮我安排一个早晚护肤流程");

        when(memoryService.listActiveMemories("u-3")).thenReturn(List.of());

        SseEmitter emitter = chatService.streamChat("u-3", request);
        assertNotNull(emitter);
        verify(memoryService, timeout(2000)).listActiveMemories("u-3");
    }
}
