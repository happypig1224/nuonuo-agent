package com.nuonuo.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云 OSS 对象存储配置
 */
@Component
@ConfigurationProperties(prefix = "nuonuo.oss")
public class OssProperties {

    private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    private String bucketName = "nuonuo-agents";
    private String accessKeyId;
    private String accessKeySecret;

    public boolean isReady() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(bucketName)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret);
    }

    public String buildPublicUrl(String objectKey) {
        String cleanEndpoint = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + cleanEndpoint + "/" + objectKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
}
