package com.nuonuo.media.service.impl;

import com.nuonuo.media.entity.MediaAssetEntity;
import com.nuonuo.media.mapper.MediaAssetMapper;
import com.nuonuo.media.service.MediaAssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 多模态素材异步持久化实现：
 * 异步解码 -> 写入对象存储（本地 dev 目录兜底） -> 异步落库 media_asset
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    private static final Logger log = LoggerFactory.getLogger(MediaAssetServiceImpl.class);
    private static final Path LOCAL_OSS_DIR = Paths.get("uploads");

    private final MediaAssetMapper mediaAssetMapper;

    public MediaAssetServiceImpl(MediaAssetMapper mediaAssetMapper) {
        this.mediaAssetMapper = mediaAssetMapper;
    }

    @Override
    public void persistAsync(String userId, String sessionId, String mediaType, String mediaTag, String base64Data, String ext) {
        if (!StringUtils.hasText(base64Data)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String raw = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(',') + 1) : base64Data;
                byte[] bytes = Base64.getDecoder().decode(raw);

                String safeExt = StringUtils.hasText(ext) ? ext.replaceAll("[^a-zA-Z0-9]", "") : "bin";
                String objectKey = userId + "/" + sessionId + "/" + UUID.randomUUID() + "." + safeExt;

                // 开发环境兜底写入本地 uploads 目录（生产环境可替换为 OSS SDK 上传）
                Path target = LOCAL_OSS_DIR.resolve(objectKey);
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);

                MediaAssetEntity entity = new MediaAssetEntity();
                entity.setUserId(userId);
                entity.setSessionId(sessionId);
                entity.setMediaType(mediaType);
                entity.setMediaTag(mediaTag);
                entity.setStorageUrl("/uploads/" + objectKey);
                entity.setFileSize((long) bytes.length);
                mediaAssetMapper.insert(entity);

                log.info("Async persisted {} asset: userId={}, sessionId={}, url={}, size={}",
                        mediaType, userId, sessionId, entity.getStorageUrl(), bytes.length);
            } catch (Exception ex) {
                log.warn("Async media persistence failed: userId={}, sessionId={}, type={}",
                        userId, sessionId, mediaType, ex);
            }
        });
    }
}
