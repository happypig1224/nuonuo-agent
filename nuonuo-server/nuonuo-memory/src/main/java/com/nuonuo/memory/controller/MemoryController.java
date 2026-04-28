package com.nuonuo.memory.controller;

import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
import com.nuonuo.memory.dto.MemoryItemResponse;
import com.nuonuo.memory.dto.UpdateMemoryRequest;
import com.nuonuo.memory.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户长期记忆控制器
 */
@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public Result<List<MemoryItemResponse>> list() {
        String userId = UserContext.getUserId();
        return Result.success(memoryService.listActiveMemories(userId));
    }

    @PatchMapping("/{id}")
    public Result<MemoryItemResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMemoryRequest request) {
        String userId = UserContext.getUserId();
        return Result.success(memoryService.updateMemory(userId, id, request.getProfileValue()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        String userId = UserContext.getUserId();
        memoryService.deleteMemory(userId, id);
        return Result.success(null);
    }

    @DeleteMapping
    public Result<Void> clearAll() {
        String userId = UserContext.getUserId();
        memoryService.clearAllMemories(userId);
        return Result.success(null);
    }
}
