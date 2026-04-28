package com.nuonuo.chat.service;

/**
 * 短期会话上下文与双阈值摘要压缩服务
 */
public interface SessionContextService {

    /**
     * 读取历史会话摘要与最近 3 轮问答，组装短期上下文
     */
    String buildShortTermContext(String sessionId);

    /**
     * 追加本轮问答并在超过阈值（>4000 Tokens 或 >15 轮）时压缩较早历史为 200 字以内摘要
     */
    void appendAndCompress(String sessionId, String userPrompt, String assistantReply);
}
