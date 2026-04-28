package com.nuonuo.chat.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.dto.ChatHistoryItem;
import com.nuonuo.chat.mapper.ChatMessageMapper;
import com.nuonuo.chat.service.ChatService;
import com.nuonuo.media.service.MediaAssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 历史会话定时归档任务：
 * 每日凌晨 3 点扫描超过 30 天未更新（MAX(gmt_modified) < now - 30d）的会话，
 * 按更新时间升序导出为标准 JSON 上传至阿里云 OSS（nuonuo-agents）。
 */
@Component
public class ChatArchiveJob {

    private static final Logger log = LoggerFactory.getLogger(ChatArchiveJob.class);
    private static final int ARCHIVE_DAYS_THRESHOLD = 30;

    private final ChatMessageMapper chatMessageMapper;
    private final ChatService chatService;
    private final MediaAssetService mediaAssetService;
    private final ObjectMapper objectMapper;

    public ChatArchiveJob(
            ChatMessageMapper chatMessageMapper,
            ChatService chatService,
            MediaAssetService mediaAssetService,
            ObjectMapper objectMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.chatService = chatService;
        this.mediaAssetService = mediaAssetService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ARCHIVE_DAYS_THRESHOLD);
        List<Map<String, Object>> expired = chatMessageMapper.findExpiredSessions(cutoff);
        if (expired == null || expired.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : expired) {
            String userId = String.valueOf(row.get("userId"));
            String sessionId = String.valueOf(row.get("sessionId"));
            try {
                List<ChatHistoryItem> items = chatService.getHistory(userId, sessionId);
                String json = objectMapper.writeValueAsString(items);
                String url = mediaAssetService.uploadJsonAndGetPresignedUrl(userId, sessionId, "history.json", json);
                log.info("Archived session history to OSS: userId={}, sessionId={}, url={}", userId, sessionId, url);
            } catch (Exception ex) {
                log.warn("Failed to archive session: userId={}, sessionId={}", userId, sessionId, ex);
            }
        }
    }
}
