package com.nuonuo.memory.service;

import com.nuonuo.memory.dto.MemoryItemResponse;

import java.util.List;

/**
 * 用户长期记忆服务
 */
public interface MemoryService {

    /**
     * 查询当前用户生效中的长期护肤记忆列表
     */
    List<MemoryItemResponse> listActiveMemories(String userId);

    /**
     * 更新指定记忆的值（校验归属权与 ACTIVE 状态）
     */
    MemoryItemResponse updateMemory(String userId, Long memoryId, String profileValue);

    /**
     * 标记指定记忆为 USER_DELETED 墓碑状态
     */
    void deleteMemory(String userId, Long memoryId);

    /**
     * 清空当前用户所有生效中的记忆（标记为 USER_DELETED 墓碑）
     */
    void clearAllMemories(String userId);
}
