package com.nuonuo.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.service.SessionContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionContextServiceImpl implements SessionContextService {

    private static final Logger log = LoggerFactory.getLogger(SessionContextServiceImpl.class);
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final int KEEP_RECENT_MESSAGES = 6;
    private static final int MAX_TURNS_THRESHOLD = 15;
    private static final int MAX_TOKENS_THRESHOLD = 4000;
    private static final int SUMMARY_MAX_CHARS = 200;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionContextServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildShortTermContext(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return "";
        }
        try {
            ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
            ListOperations<String, String> listOps = redisTemplate.opsForList();
            String summary = valueOps.get(summaryKey(sessionId));
            List<String> recentRaw = listOps.range(recentKey(sessionId), -KEEP_RECENT_MESSAGES, -1);

            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(summary)) {
                sb.append("【历史会话摘要】\n").append(summary.trim()).append("\n\n");
            }
            if (recentRaw != null && !recentRaw.isEmpty()) {
                String recentText = recentRaw.stream()
                        .map(this::formatTurnLine)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining("\n"));
                if (StringUtils.hasText(recentText)) {
                    sb.append("【近期对话】\n").append(recentText).append("\n\n");
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("Failed to load short-term context for sessionId={}", sessionId, ex);
            return "";
        }
    }

    @Override
    public void appendAndCompress(String sessionId, String userPrompt, String assistantReply) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(userPrompt) || !StringUtils.hasText(assistantReply)) {
            return;
        }
        try {
            ListOperations<String, String> listOps = redisTemplate.opsForList();
            String rKey = recentKey(sessionId);
            listOps.rightPush(rKey, objectMapper.writeValueAsString(Map.of("role", "user", "content", userPrompt)));
            listOps.rightPush(rKey, objectMapper.writeValueAsString(Map.of("role", "assistant", "content", assistantReply)));
            redisTemplate.expire(rKey, SESSION_TTL);

            List<String> allMessages = listOps.range(rKey, 0, -1);
            if (allMessages == null || allMessages.size() <= KEEP_RECENT_MESSAGES) {
                return;
            }
            int turns = allMessages.size() / 2;
            int estimatedTokens = allMessages.stream().mapToInt(String::length).sum();
            if (turns > MAX_TURNS_THRESHOLD || estimatedTokens > MAX_TOKENS_THRESHOLD) {
                int splitIndex = allMessages.size() - KEEP_RECENT_MESSAGES;
                List<String> older = allMessages.subList(0, splitIndex);
                String compressed = compressOlderMessages(older);
                redisTemplate.opsForValue().set(summaryKey(sessionId), compressed, SESSION_TTL);
                listOps.trim(rKey, -KEEP_RECENT_MESSAGES, -1);
            }
        } catch (Exception ex) {
            log.warn("Failed to append/compress session context for sessionId={}", sessionId, ex);
        }
    }

    private String compressOlderMessages(List<String> older) {
        String joined = older.stream()
                .map(this::formatTurnLine)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("；"));
        if (joined.length() <= SUMMARY_MAX_CHARS) {
            return joined;
        }
        return joined.substring(0, SUMMARY_MAX_CHARS - 3) + "...";
    }

    private String formatTurnLine(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String role = node.path("role").asText("user");
            String content = node.path("content").asText("");
            return ("assistant".equals(role) ? "小糯: " : "用户: ") + content;
        } catch (Exception ex) {
            return "";
        }
    }

    private String summaryKey(String sessionId) {
        return "nuonuo:session:" + sessionId + ":summary";
    }

    private String recentKey(String sessionId) {
        return "nuonuo:session:" + sessionId + ":recent";
    }
}
