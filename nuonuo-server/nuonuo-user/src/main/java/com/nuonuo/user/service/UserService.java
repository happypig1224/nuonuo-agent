package com.nuonuo.user.service;

import com.nuonuo.user.dto.LoginRequest;
import com.nuonuo.user.dto.RegisterRequest;
import com.nuonuo.user.dto.UpdateProfileRequest;
import com.nuonuo.user.dto.UserProfileResponse;

/**
 * 用户账户与个人资料服务接口。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
public interface UserService {

    /**
     * 使用账号与密码注册新账号，并签发会话 Token。
     *
     * @param request 注册请求参数
     * @return 用户档案与会话 Token
     */
    UserProfileResponse register(RegisterRequest request);

    /**
     * 使用账号与密码登录，并签发会话 Token。
     *
     * @param request 登录请求参数
     * @return 用户档案与会话 Token
     */
    UserProfileResponse login(LoginRequest request);

    /**
     * 获取当前已认证用户的个人档案。
     *
     * @return 用户档案
     */
    UserProfileResponse getProfile();

    /**
     * 更新当前已认证用户的个人昵称。
     *
     * @param request 更新请求参数
     * @return 更新后的用户档案
     */
    UserProfileResponse updateProfile(UpdateProfileRequest request);

    /**
     * 退出登录并吊销当前会话 Token。
     *
     * @param token 当前请求携带的 Bearer Token
     */
    void logout(String token);

    /**
     * 注销当前用户账号并吊销会话 Token。
     *
     * @param token 当前请求携带的 Bearer Token
     */
    void deleteAccount(String token);
}
