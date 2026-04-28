package com.nuonuo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nuonuo.common.constant.RedisConstant;
import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;
import com.nuonuo.common.util.RedisUtils;
import com.nuonuo.user.dto.LoginRequest;
import com.nuonuo.user.dto.RegisterRequest;
import com.nuonuo.user.dto.SendOtpRequest;
import com.nuonuo.user.dto.UpdateProfileRequest;
import com.nuonuo.user.dto.UserProfileResponse;
import com.nuonuo.user.entity.UserAccountEntity;
import com.nuonuo.user.mapper.UserAccountMapper;
import com.nuonuo.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 用户账户与个人资料服务实现。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 10_000;
    private static final int PBKDF2_KEY_LENGTH = 256;

    private final UserAccountMapper userAccountMapper;

    public UserServiceImpl(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public UserProfileResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "两次输入的密码不一致");
        }

        String normalizedUserId = request.getAccount().trim().toLowerCase();
        if (findUserByUserId(normalizedUserId) != null) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "该账号已注册，请直接登录");
        }

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(normalizedUserId);
        String defaultNickname = request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim()
                : "小糯新朋友";
        entity.setNickname(defaultNickname);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        userAccountMapper.insert(entity);

        return issueTokenResponse(entity);
    }

    @Override
    public UserProfileResponse login(LoginRequest request) {
        String normalizedUserId = request.getAccount().trim().toLowerCase();
        UserAccountEntity entity = findUserByUserId(normalizedUserId);
        if (entity == null || !verifyPassword(request.getPassword(), entity.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return issueTokenResponse(entity);
    }

    @Override
    public UserProfileResponse getProfile() {
        String userId = UserContext.getRequiredUserId();
        UserAccountEntity entity = findRequiredUser(userId);
        return new UserProfileResponse(
                entity.getUserId(),
                entity.getNickname(),
                null,
                entity.getGmtCreate()
        );
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        String userId = UserContext.getRequiredUserId();
        UserAccountEntity entity = findRequiredUser(userId);
        entity.setNickname(request.getNickname().trim());
        userAccountMapper.updateById(entity);

        return new UserProfileResponse(
                entity.getUserId(),
                entity.getNickname(),
                null,
                entity.getGmtCreate()
        );
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            RedisUtils.delete(RedisConstant.AUTH_TOKEN_KEY_PREFIX + token.trim());
        }
    }

    @Override
    public void deleteAccount(String token) {
        String userId = UserContext.getRequiredUserId();
        UserAccountEntity entity = findRequiredUser(userId);
        userAccountMapper.deleteById(entity.getId());
        logout(token);
    }

    private UserProfileResponse issueTokenResponse(UserAccountEntity entity) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RedisUtils.set(RedisConstant.AUTH_TOKEN_KEY_PREFIX + token, entity.getUserId(), RedisConstant.AUTH_TOKEN_TTL);
        return new UserProfileResponse(
                entity.getUserId(),
                entity.getNickname(),
                token,
                entity.getGmtCreate()
        );
    }

    private UserAccountEntity findUserByUserId(String userId) {
        return userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccountEntity>()
                        .eq(UserAccountEntity::getUserId, userId)
                        .last("LIMIT 1")
        );
    }

    private UserAccountEntity findRequiredUser(String userId) {
        UserAccountEntity entity = findUserByUserId(userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户账号不存在");
        }
        return entity;
    }

    private String hashPassword(String rawPassword) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        String[] parts = storedHash.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
