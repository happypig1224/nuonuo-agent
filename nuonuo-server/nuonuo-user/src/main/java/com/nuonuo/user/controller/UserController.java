package com.nuonuo.user.controller;

import com.nuonuo.common.model.Result;
import com.nuonuo.user.dto.LoginRequest;
import com.nuonuo.user.dto.RegisterRequest;
import com.nuonuo.user.dto.UpdateProfileRequest;
import com.nuonuo.user.dto.UserProfileResponse;
import com.nuonuo.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证与个人资料控制器。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 使用账号与密码注册新账号。
     *
     * @param request 注册请求体
     * @return 用户档案与会话 Token
     */
    @PostMapping("/register")
    public Result<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    /**
     * 使用账号与密码登录。
     *
     * @param request 登录请求体
     * @return 用户档案与会话 Token
     */
    @PostMapping("/login")
    public Result<UserProfileResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 获取当前已认证用户的个人档案。
     *
     * @return 用户档案
     */
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        return Result.success(userService.getProfile());
    }

    /**
     * 更新当前已认证用户的昵称。
     *
     * @param request 更新请求体
     * @return 更新后的用户档案
     */
    @PatchMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(request));
    }

    /**
     * 退出登录并吊销当前会话 Token。
     *
     * @param request HTTP 请求
     * @return 空成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        userService.logout(resolveBearerToken(request));
        return Result.success();
    }

    /**
     * 永久注销当前用户账号并吊销会话 Token。
     *
     * @param request HTTP 请求
     * @return 空成功响应
     */
    @DeleteMapping("/account")
    public Result<Void> deleteAccount(HttpServletRequest request) {
        userService.deleteAccount(resolveBearerToken(request));
        return Result.success();
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
