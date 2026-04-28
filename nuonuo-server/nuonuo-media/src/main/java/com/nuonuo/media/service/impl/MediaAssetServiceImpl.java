package com.nuonuo.media.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.nuonuo.media.config.OssProperties;
import com.nuonuo.media.entity.MediaAssetEntity;
import com.nuonuo.media.mapper.MediaAssetMapper;
import com.nuonuo.media.service.MediaAssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 多模态素材异步持久化实现：
 * 异步解码 -> 优先上传阿里云 OSS（nuonuo-agents），未配置密钥时降级本地 uploads 目录 -> 异步落库 media_asset
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    private static final Logger log = LoggerFactory.getLogger(MediaAssetServiceImpl.class);
    private static final Path LOCAL_OSS_DIR = Paths.get("uploads");

    private final MediaAssetMapper mediaAssetMapper;
    private final OssProperties ossProperties;

    @Autowired
    public MediaAssetServiceImpl(MediaAssetMapper mediaAssetMapper, OssProperties ossProperties) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.ossProperties = ossProperties;
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

                String objectKey = buildObjectKey(userId, sessionId, mediaType, ext);
                String storageUrl = uploadOrFallback(objectKey, bytes);

                MediaAssetEntity entity = new MediaAssetEntity();
                entity.setUserId(userId);
                entity.setSessionId(sessionId);
                entity.setMediaType(mediaType);
                entity.setMediaTag(mediaTag);
                entity.setStorageUrl(storageUrl);
                entity.setFileSize((long) bytes.length);
                mediaAssetMapper.insert(entity);

                log.info("Async persisted {} asset: userId={}, sessionId={}, url={}, size={}",
                        mediaType, userId, sessionId, storageUrl, bytes.length);
            } catch (Exception ex) {
                log.warn("Async media persistence failed: userId={}, sessionId={}, type={}",
                        userId, sessionId, mediaType, ex);
            }
        });
    }

    @Override
    public String uploadJsonAndGetPresignedUrl(String userId, String sessionId, String fileName, String jsonContent) {
        String safeUser = sanitizeSegment(userId, "anonymous");
        String safeSession = sanitizeSegment(sessionId, "default");
        String safeFile = StringUtils.hasText(fileName) ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "history.json";
        String objectKey = "users/" + safeUser + "/sessions/" + safeSession + "/" + safeFile;
        byte[] bytes = (jsonContent != null ? jsonContent : "[]").getBytes(StandardCharsets.UTF_8);

        try {
            String url;
            if (ossProperties != null && ossProperties.isReady()) {
                OSS ossClient = new OSSClientBuilder().build(
                        ossProperties.getEndpoint(),
                        ossProperties.getAccessKeyId(),
                        ossProperties.getAccessKeySecret()
                );
                try {
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentType("application/json; charset=utf-8");
                    metadata.setContentLength(bytes.length);
                    ossClient.putObject(ossProperties.getBucketName(), objectKey, new ByteArrayInputStream(bytes), metadata);
                    Date expiration = new Date(System.currentTimeMillis() + 15 * 60 * 1000L);
                    url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expiration).toString();
                } finally {
                    ossClient.shutdown();
                }
            } else {
                Path target = LOCAL_OSS_DIR.resolve(objectKey);
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
                url = "/uploads/" + objectKey;
            }

            MediaAssetEntity entity = new MediaAssetEntity();
            entity.setUserId(userId);
            entity.setSessionId(sessionId);
            entity.setMediaType("HISTORY_JSON");
            entity.setMediaTag("archive");
            entity.setStorageUrl(url);
            entity.setFileSize((long) bytes.length);
            mediaAssetMapper.insert(entity);
            return url;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload session history JSON to OSS", ex);
        }
    }

    String buildObjectKey(String userId, String sessionId, String mediaType, String ext) {
        String safeUser = sanitizeSegment(userId, "anonymous");
        String safeSession = sanitizeSegment(sessionId, "default");
        String safeType = sanitizeSegment(mediaType, "media").toLowerCase(Locale.ROOT);
        String safeExt = StringUtils.hasText(ext) ? ext.replaceAll("[^a-zA-Z0-9]", "") : "bin";
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "users/" + safeUser + "/sessions/" + safeSession + "/" + safeType + "/" + datePath + "/" + UUID.randomUUID() + "." + safeExt;
    }

    private String sanitizeSegment(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    String uploadOrFallback(String objectKey, byte[] bytes) throws Exception {
        if (ossProperties != null && ossProperties.isReady()) {
            OSS ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );
            try {
                ossClient.putObject(ossProperties.getBucketName(), objectKey, new ByteArrayInputStream(bytes));
                return ossProperties.buildPublicUrl(objectKey);
            } finally {
                ossClient.shutdown();
            }
        }
        Path target = LOCAL_OSS_DIR.resolve(objectKey);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return "/uploads/" + objectKey;
    }
}
