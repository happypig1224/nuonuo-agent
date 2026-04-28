package com.nuonuo.ingredient.controller;

import com.nuonuo.common.context.UserContext;
import com.nuonuo.common.model.Result;
import com.nuonuo.ingredient.dto.IngredientCheckRequest;
import com.nuonuo.ingredient.dto.IngredientCheckResponse;
import com.nuonuo.ingredient.service.IngredientCheckService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成分配伍禁忌校验控制器
 */
@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private final IngredientCheckService ingredientCheckService;

    public IngredientController(IngredientCheckService ingredientCheckService) {
        this.ingredientCheckService = ingredientCheckService;
    }

    @PostMapping("/check")
    public Result<IngredientCheckResponse> check(@Valid @RequestBody IngredientCheckRequest request) {
        String userId = UserContext.getUserId();
        return Result.success(ingredientCheckService.check(userId, request.getSessionId(), request.getText()));
    }
}
