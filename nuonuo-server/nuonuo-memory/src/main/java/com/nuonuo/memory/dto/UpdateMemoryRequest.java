package com.nuonuo.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新用户记忆请求 DTO
 */
public class UpdateMemoryRequest {

    @NotBlank(message = "记忆内容不能为空")
    @Size(max = 255, message = "记忆内容不能超过 255 字")
    private String profileValue;

    public String getProfileValue() {
        return profileValue;
    }

    public void setProfileValue(String profileValue) {
        this.profileValue = profileValue;
    }
}
