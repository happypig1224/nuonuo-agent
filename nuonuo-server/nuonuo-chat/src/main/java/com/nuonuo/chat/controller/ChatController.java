package com.nuonuo.chat.controller;

import com.nuonuo.chat.dto.ChatHistoryItem;
import com.nuonuo.chat.dto.ChatStreamRequest;
import com.nuonuo.chat.service.ChatService;
import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
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

/**
 * 多模态流式对话控制器
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
}
