package com.nuonuo.ingredient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 成分配伍禁忌校验请求 DTO
 */
public class IngredientCheckRequest {

    @NotBlank(message = "待检测文本不能为空")
    @Size(max = 5000, message = "待检测文本不能超过 5000 字")
    private String text;

    @Size(max = 64, message = "会话 ID 长度不能超过 64")
    private String sessionId;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
