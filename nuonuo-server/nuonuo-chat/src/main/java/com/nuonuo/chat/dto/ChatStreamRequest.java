package com.nuonuo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 多模态流式对话请求 DTO
 */
public class ChatStreamRequest {

    @NotBlank(message = "会话 ID 不能为空")
    @Size(max = 64, message = "会话 ID 长度不能超过 64")
    private String sessionId;

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 2000, message = "提问内容不能超过 2000 字")
    private String prompt;

    @Size(max = 128, message = "图片文件 ID 长度不能超过 128")
    private String imageFileId;

    @Size(max = 4_000_000, message = "图片 Base64 数据过大")
    private String imageBase64;

    @Size(max = 8_000_000, message = "语音 Base64 数据过大")
    private String audioBase64;

    @Size(max = 16, message = "语音格式长度不能超过 16")
    private String audioFormat;

    @Size(max = 32, message = "图片标签长度不能超过 32")
    private String imageTag;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageFileId() {
        return imageFileId;
    }

    public void setImageFileId(String imageFileId) {
        this.imageFileId = imageFileId;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }
}
