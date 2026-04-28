package com.nuonuo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送注册验证码请求 DTO。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public class SendOtpRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需在 3 到 64 个字符之间")
    private String account;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
