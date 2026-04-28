package com.nuonuo.media.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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

                String safeExt = StringUtils.hasText(ext) ? ext.replaceAll("[^a-zA-Z0-9]", "") : "bin";
                String objectKey = userId + "/" + sessionId + "/" + UUID.randomUUID() + "." + safeExt;
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
