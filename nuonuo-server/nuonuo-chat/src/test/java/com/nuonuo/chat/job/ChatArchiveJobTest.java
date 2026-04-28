package com.nuonuo.chat.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.chat.dto.ChatHistoryItem;
import com.nuonuo.chat.mapper.ChatMessageMapper;
import com.nuonuo.chat.service.ChatService;
import com.nuonuo.media.service.MediaAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatArchiveJobTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatService chatService;
    @Mock
    private MediaAssetService mediaAssetService;

    private ChatArchiveJob archiveJob;

    @BeforeEach
    void setUp() {
        archiveJob = new ChatArchiveJob(chatMessageMapper, chatService, mediaAssetService, new ObjectMapper());
    }

    @Test
    void archiveExpiredSessions_shouldUploadJson_whenSessionsOlderThan30DaysExist() {
        when(chatMessageMapper.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(List.of(Map.of("userId", "u1", "sessionId", "s-old")));
        when(chatService.getHistory("u1", "s-old"))
                .thenReturn(List.of(new ChatHistoryItem("m1", "user", "hi", null)));

        archiveJob.archiveExpiredSessions();

        verify(mediaAssetService).uploadJsonAndGetPresignedUrl(eq("u1"), eq("s-old"), eq("history.json"), anyString());
    }

    @Test
    void archiveExpiredSessions_shouldDoNothing_whenNoExpiredSessions() {
        when(chatMessageMapper.findExpiredSessions(any(LocalDateTime.class))).thenReturn(List.of());

        archiveJob.archiveExpiredSessions();

        verify(mediaAssetService, never()).uploadJsonAndGetPresignedUrl(anyString(), anyString(), anyString(), anyString());
    }
}
