package com.nuonuo.memory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户长期记忆返回 DTO
 */
public class MemoryItemResponse {

    private String id;
    private String category;
    private String profileKey;
    private String profileValue;
    private BigDecimal confidence;
    private LocalDateTime updatedAt;

    public MemoryItemResponse() {
    }

    public MemoryItemResponse(
            String id,
            String category,
            String profileKey,
            String profileValue,
            BigDecimal confidence,
            LocalDateTime updatedAt) {
        this.id = id;
        this.category = category;
        this.profileKey = profileKey;
        this.profileValue = profileValue;
        this.confidence = confidence;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public void setProfileKey(String profileKey) {
        this.profileKey = profileKey;
    }

    public String getProfileValue() {
        return profileValue;
    }

    public void setProfileValue(String profileValue) {
        this.profileValue = profileValue;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
