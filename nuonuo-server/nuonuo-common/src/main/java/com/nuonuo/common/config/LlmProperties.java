package com.nuonuo.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型调用配置（OpenAI 兼容协议 + 重试与备用模型兜底）
 */
@Component
@ConfigurationProperties(prefix = "nuonuo.llm")
public class LlmProperties {

    private String baseUrl = "https://api.302.ai/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private String fallbackModel = "gpt-4o-mini";
    private int maxRetries = 3;
    private long initialBackoffMs = 300L;
    private long maxBackoffMs = 3000L;
    private String promptLocation = "classpath*:prompts/*.xml";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }

    public String getPromptLocation() {
        return promptLocation;
    }

    public void setPromptLocation(String promptLocation) {
        this.promptLocation = promptLocation;
    }
}
