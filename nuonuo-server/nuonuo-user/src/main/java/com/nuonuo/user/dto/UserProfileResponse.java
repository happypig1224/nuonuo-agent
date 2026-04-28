package com.nuonuo.user.dto;

import java.time.LocalDateTime;

/**
 * 用户个人档案响应 DTO。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public class UserProfileResponse {

    private String userId;
    private String nickname;
    private String token;
    private LocalDateTime gmtCreate;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String userId, String nickname, String token, LocalDateTime gmtCreate) {
        this.userId = userId;
        this.nickname = nickname;
        this.token = token;
        this.gmtCreate = gmtCreate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}
