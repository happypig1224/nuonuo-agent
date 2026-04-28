package com.nuonuo.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.dto.ChatHistoryItem;
import com.nuonuo.chat.dto.ChatStreamRequest;
import com.nuonuo.chat.entity.ChatMessageEntity;
import com.nuonuo.chat.mapper.ChatMessageMapper;
import com.nuonuo.chat.service.ChatService;
import com.nuonuo.common.utils.LlmClient;
import com.nuonuo.media.service.MediaAssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 多模态对话最小闭环实现：
 * 请求承接 -> 结构化 -> 提示词组装（XML） -> 调用 LlmClient -> SSE 响应
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String CHAT_SYSTEM_PROMPT_ID = "chat.system";

    private final LlmClient llmClient;
    private final MediaAssetService mediaAssetService;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(
            LlmClient llmClient,
            MediaAssetService mediaAssetService,
            ChatMessageMapper chatMessageMapper,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.mediaAssetService = mediaAssetService;
        this.chatMessageMapper = chatMessageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChatHistoryItem> getHistory(String userId, String sessionId) {
        List<ChatMessageEntity> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getUserId, userId)
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .orderByAsc(ChatMessageEntity::getGmtCreate)
                        .last("LIMIT 100")
        );
        return list.stream()
                .map(e -> new ChatHistoryItem(e.getMessageId(), e.getRole(), e.getContent(), e.getGmtCreate()))
                .toList();
    }

    @Override
    public SseEmitter streamChat(String userId, ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        // 异步解耦：原始图片/语音上传对象存储并落库，不阻塞主链路 SSE 响应
        mediaAssetService.persistAsync(userId, request.getSessionId(), "IMAGE", request.getImageTag(), request.getImageBase64(), "png");
        mediaAssetService.persistAsync(userId, request.getSessionId(), "AUDIO", "voice", request.getAudioBase64(), request.getAudioFormat());

        CompletableFuture.runAsync(() -> {
            try {
                // 1. 结构化用户输入
                String structuredUserPrompt = buildStructuredPrompt(request);

                // 2. 推送初始情绪状态
                sendEvent(emitter, "emotion", Map.of("emotion", "thinking"));

                StringBuilder replyBuilder = new StringBuilder();
                // 3. 调用 LlmClient（含 XML 提示词加载、指数退避+抖动重试、备用模型兜底）
                if (llmClient.isConfigured()) {
                    llmClient.streamChat(
                            CHAT_SYSTEM_PROMPT_ID,
                            structuredUserPrompt,
                            request.getImageBase64(),
                            request.getAudioBase64(),
                            request.getAudioFormat(),
                            chunk -> {
                                replyBuilder.append(chunk);
                                try {
                                    sendEvent(emitter, "delta", Map.of("content", chunk));
                                } catch (IOException e) {
                                    throw new RuntimeException("SSE send failed", e);
                                }
                            });
                } else {
                    streamFallbackReply(emitter, request);
                }

                // 4. 结束事件
                sendEvent(emitter, "emotion", Map.of("emotion", "happy"));
                sendEvent(emitter, "done", Map.of("sessionId", request.getSessionId()));
                emitter.complete();

                // 5. 异步保存用户提问与助手回复到 chat_message
                String finalReply = replyBuilder.toString();
                if (StringUtils.hasText(finalReply)) {
                    saveMessageAsync(userId, request.getSessionId(), "user", request.getPrompt().trim());
                    saveMessageAsync(userId, request.getSessionId(), "assistant", finalReply);
                }
            } catch (Exception ex) {
                log.error("SSE chat stream failed for userId={}, sessionId={}", userId, request.getSessionId(), ex);
                try {
                    sendEvent(emitter, "error", Map.of("message", "小糯刚才开小差了，请稍后再试"));
                } catch (IOException ignored) {
                    // 连接已断开时忽略二次发送异常
                }
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    private String buildStructuredPrompt(ChatStreamRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户问题】\n").append(request.getPrompt().trim());
        if (StringUtils.hasText(request.getImageFileId())) {
            sb.append("\n\n【附带图片】\n")
              .append("fileId: ").append(request.getImageFileId());
            if (StringUtils.hasText(request.getImageTag())) {
                sb.append(", 场景标签: ").append(request.getImageTag());
            }
        }
        return sb.toString();
    }

    private void streamFallbackReply(SseEmitter emitter, ChatStreamRequest request) throws IOException, InterruptedException {
        String reply = "收到你的问题啦：「" + request.getPrompt().trim() + "」。"
                + (StringUtils.hasText(request.getImageFileId())
                        ? "我也看到你附带了图片（" + request.getImageFileId() + "）。"
                        : "")
                + "当前模型接口尚未配置 API Key，已为你跑通请求承接、XML 提示词加载与 SSE 流式响应链路。";

        int chunkSize = 8;
        for (int i = 0; i < reply.length(); i += chunkSize) {
            String chunk = reply.substring(i, Math.min(i + chunkSize, reply.length()));
            sendEvent(emitter, "delta", Map.of("content", chunk));
            Thread.sleep(20);
        }
    }

    private void saveMessageAsync(String userId, String sessionId, String role, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                ChatMessageEntity entity = new ChatMessageEntity();
                entity.setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""));
                entity.setUserId(userId);
                entity.setSessionId(sessionId);
                entity.setRole(role);
                entity.setContent(content);
                chatMessageMapper.insert(entity);
            } catch (Exception ex) {
                log.warn("Async save chat_message failed: userId={}, sessionId={}, role={}", userId, sessionId, role, ex);
            }
        });
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(objectMapper.writeValueAsString(data)));
    }
}
