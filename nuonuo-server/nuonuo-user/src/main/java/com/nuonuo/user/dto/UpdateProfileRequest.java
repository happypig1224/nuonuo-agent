package com.nuonuo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新用户个人资料请求 DTO。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public class UpdateProfileRequest {

    /**
     * 新用户昵称。
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 32, message = "昵称长度需在 1 到 32 个字符之间")
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
