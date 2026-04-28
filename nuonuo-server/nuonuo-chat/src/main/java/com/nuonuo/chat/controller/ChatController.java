package com.nuonuo.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.dto.ChatHistoryItem;
import com.nuonuo.chat.dto.ChatStreamRequest;
import com.nuonuo.chat.service.ChatService;
import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
import com.nuonuo.media.service.MediaAssetService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 多模态流式对话控制器
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final MediaAssetService mediaAssetService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, MediaAssetService mediaAssetService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.mediaAssetService = mediaAssetService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        String userId = UserContext.getUserId();
        return chatService.streamChat(userId, request);
    }

    @GetMapping("/history")
    public Result<List<ChatHistoryItem>> history(@RequestParam("sessionId") String sessionId) {
        String userId = UserContext.getUserId();
        return Result.success(chatService.getHistory(userId, sessionId));
    }

    @GetMapping("/history/archive-url")
    public Result<Map<String, String>> archiveUrl(@RequestParam("sessionId") String sessionId) throws Exception {
        String userId = UserContext.getUserId();
        List<ChatHistoryItem> items = chatService.getHistory(userId, sessionId);
        String json = objectMapper.writeValueAsString(items);
        String url = mediaAssetService.uploadJsonAndGetPresignedUrl(userId, sessionId, "history.json", json);
        return Result.success(Map.of(
                "sessionId", sessionId,
                "format", "json",
                "url", url
        ));
    }
}
