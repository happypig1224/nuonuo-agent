package com.nuonuo.memory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nuonuo.common.exception.BusinessException;
import com.nuonuo.common.exception.ErrorCode;
import com.nuonuo.memory.dto.MemoryItemResponse;
import com.nuonuo.memory.entity.UserProfileMemoryEntity;
import com.nuonuo.memory.mapper.UserProfileMemoryMapper;
import com.nuonuo.memory.service.MemoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户长期记忆服务实现
 */
@Service
public class MemoryServiceImpl implements MemoryService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_USER_DELETED = "USER_DELETED";

    private final UserProfileMemoryMapper memoryMapper;

    public MemoryServiceImpl(UserProfileMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    @Override
    public List<MemoryItemResponse> listActiveMemories(String userId) {
        List<UserProfileMemoryEntity> entities = memoryMapper.selectList(
                new LambdaQueryWrapper<UserProfileMemoryEntity>()
                        .eq(UserProfileMemoryEntity::getUserId, userId)
                        .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
                        .orderByDesc(UserProfileMemoryEntity::getGmtModified)
        );
        return entities.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemoryItemResponse updateMemory(String userId, Long memoryId, String profileValue) {
        UserProfileMemoryEntity existing = memoryMapper.selectOne(
                new LambdaQueryWrapper<UserProfileMemoryEntity>()
                        .eq(UserProfileMemoryEntity::getId, memoryId)
                        .eq(UserProfileMemoryEntity::getUserId, userId)
                        .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
        );
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "记忆不存在或无权操作");
        }

        int updated = memoryMapper.update(
                null,
                new LambdaUpdateWrapper<UserProfileMemoryEntity>()
                        .eq(UserProfileMemoryEntity::getId, memoryId)
                        .eq(UserProfileMemoryEntity::getUserId, userId)
                        .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
                        .set(UserProfileMemoryEntity::getProfileValue, profileValue.trim())
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "记忆状态已变更，请刷新重试");
        }
        existing.setProfileValue(profileValue.trim());
        return toResponse(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemory(String userId, Long memoryId) {
        int updated = memoryMapper.update(
                null,
                new LambdaUpdateWrapper<UserProfileMemoryEntity>()
                        .eq(UserProfileMemoryEntity::getId, memoryId)
                        .eq(UserProfileMemoryEntity::getUserId, userId)
                        .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
                        .set(UserProfileMemoryEntity::getStatus, STATUS_USER_DELETED)
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "记忆不存在或已删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllMemories(String userId) {
        memoryMapper.update(
                null,
                new LambdaUpdateWrapper<UserProfileMemoryEntity>()
                        .eq(UserProfileMemoryEntity::getUserId, userId)
                        .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
                        .set(UserProfileMemoryEntity::getStatus, STATUS_USER_DELETED)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MemoryItemResponse> extractAndUpsert(String userId, String userText) {
        if (userText == null || userText.isBlank()) {
            return List.of();
        }
        List<String[]> candidates = new java.util.ArrayList<>();
        String[] skinTypes = {"干敏肌", "油痘肌", "混油皮", "混干皮", "干皮", "油皮", "敏感肌", "中性肌"};
        for (String st : skinTypes) {
            if (userText.contains(st)) {
                candidates.add(new String[]{"SKIN_PROFILE", "SKIN_TYPE", st});
                break;
            }
        }
        String[] intolerances = {"烟酰胺不耐受", "酒精过敏", "香精过敏", "刷酸不耐受", "A醇不耐受"};
        for (String it : intolerances) {
            if (userText.contains(it)) {
                candidates.add(new String[]{"ALLERGY_INTOLERANCE", "INTOLERANCE", it});
                break;
            }
        }
        String[] goals = {"抗老", "美白", "祛痘", "保湿", "修护屏障", "淡斑"};
        for (String g : goals) {
            if (userText.contains(g)) {
                candidates.add(new String[]{"SKINCARE_GOAL", "GOAL", g});
                break;
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<MemoryItemResponse> results = new java.util.ArrayList<>();
        for (String[] c : candidates) {
            UserProfileMemoryEntity existing = memoryMapper.selectOne(
                    new LambdaQueryWrapper<UserProfileMemoryEntity>()
                            .eq(UserProfileMemoryEntity::getUserId, userId)
                            .eq(UserProfileMemoryEntity::getCategory, c[0])
                            .eq(UserProfileMemoryEntity::getProfileKey, c[1])
                            .eq(UserProfileMemoryEntity::getStatus, STATUS_ACTIVE)
                            .last("LIMIT 1")
            );
            if (existing != null) {
                if (!c[2].equals(existing.getProfileValue())) {
                    existing.setProfileValue(c[2]);
                    memoryMapper.updateById(existing);
                    results.add(toResponse(existing));
                }
            } else {
                UserProfileMemoryEntity entity = new UserProfileMemoryEntity();
                entity.setUserId(userId);
                entity.setCategory(c[0]);
                entity.setProfileKey(c[1]);
                entity.setProfileValue(c[2]);
                entity.setConfidence(new java.math.BigDecimal("0.90"));
                entity.setStatus(STATUS_ACTIVE);
                memoryMapper.insert(entity);
                results.add(toResponse(entity));
            }
        }
        return results;
    }

    private MemoryItemResponse toResponse(UserProfileMemoryEntity entity) {
        return new MemoryItemResponse(
                String.valueOf(entity.getId()),
                entity.getCategory(),
                entity.getProfileKey(),
                entity.getProfileValue(),
                entity.getConfidence(),
                entity.getGmtModified()
        );
    }
}
