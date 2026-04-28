package com.nuonuo.chat.service;

import com.nuonuo.chat.dto.ChatStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 多模态对话编排服务
 */
public interface ChatService {

    /**
     * 承接多模态请求并以 SSE 流式返回分析结果
     */
    SseEmitter streamChat(String userId, ChatStreamRequest request);
}
