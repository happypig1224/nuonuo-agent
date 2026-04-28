package com.nuonuo.ingredient.service;

import com.nuonuo.ingredient.dto.IngredientCheckResponse;

/**
 * 成分配伍禁忌四层防御服务
 */
public interface IngredientCheckService {

    /**
     * 对输入文本执行四层纵深防御校验：
     * L1 单体精准查表 -> L2 大类理化推断 -> L3 保守医学守卫 -> L4 异步审计哨兵
     */
    IngredientCheckResponse check(String userId, String sessionId, String text);
}
