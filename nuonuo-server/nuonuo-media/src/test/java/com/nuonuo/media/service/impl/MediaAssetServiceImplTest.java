package com.nuonuo.media.service.impl;

import com.nuonuo.media.config.OssProperties;
import com.nuonuo.media.entity.MediaAssetEntity;
import com.nuonuo.media.mapper.MediaAssetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceImplTest {

    @Mock
    private MediaAssetMapper mediaAssetMapper;

    @Test
    void ossProperties_shouldDefaultToNuonuoAgentsBucket() {
        OssProperties props = new OssProperties();
        assertEquals("nuonuo-agents", props.getBucketName());
        assertFalse(props.isReady());

        props.setAccessKeyId("ak");
        props.setAccessKeySecret("sk");
        assertTrue(props.isReady());
        assertEquals("https://nuonuo-agents.oss-cn-hangzhou.aliyuncs.com/u1/s1/a.png",
                props.buildPublicUrl("u1/s1/a.png"));
    }

    @Test
    void persistAsync_shouldFallbackToLocalUploads_whenOssKeysMissing() {
        OssProperties props = new OssProperties();
        MediaAssetServiceImpl service = new MediaAssetServiceImpl(mediaAssetMapper, props);
        String b64 = Base64.getEncoder().encodeToString("hello".getBytes());

        service.persistAsync("u-oss-test", "s-oss-test", "IMAGE", "ingredient", b64, "png");

        verify(mediaAssetMapper, timeout(2000)).insert(any(MediaAssetEntity.class));
    }

    @Test
    void buildObjectKey_shouldIsolateByUserAndSession() {
        MediaAssetServiceImpl service = new MediaAssetServiceImpl(mediaAssetMapper, new OssProperties());
        String key = service.buildObjectKey("user@nuonuo.ai", "sess-1", "IMAGE", "png");
        assertTrue(key.startsWith("users/user_nuonuo_ai/sessions/sess-1/image/"));
        assertTrue(key.endsWith(".png"));
    }

    @Test
    void uploadJsonAndGetPresignedUrl_shouldReturnJsonPath_whenOssNotConfigured() {
        MediaAssetServiceImpl service = new MediaAssetServiceImpl(mediaAssetMapper, new OssProperties());
        String url = service.uploadJsonAndGetPresignedUrl("u1", "s1", "history.json", "[{\"role\":\"user\",\"content\":\"hi\"}]");
        assertTrue(url.contains("users/u1/sessions/s1/"));
        assertTrue(url.endsWith("history.json"));
    }
}
