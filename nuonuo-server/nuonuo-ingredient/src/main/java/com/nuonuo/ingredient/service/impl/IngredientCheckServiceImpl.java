package com.nuonuo.ingredient.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.ingredient.dto.IngredientCheckResponse;
import com.nuonuo.ingredient.dto.IngredientCheckResponse.ConflictDetail;
import com.nuonuo.ingredient.entity.IngredientAuditLogEntity;
import com.nuonuo.ingredient.entity.IngredientConflictRuleEntity;
import com.nuonuo.ingredient.entity.IngredientDictEntity;
import com.nuonuo.ingredient.mapper.IngredientAuditLogMapper;
import com.nuonuo.ingredient.mapper.IngredientConflictRuleMapper;
import com.nuonuo.ingredient.mapper.IngredientDictMapper;
import com.nuonuo.ingredient.service.IngredientCheckService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成分配伍禁忌四层纵深防御实现
 */
@Service
public class IngredientCheckServiceImpl implements IngredientCheckService {

    private static final Logger log = LoggerFactory.getLogger(IngredientCheckServiceImpl.class);
    private static final String RULE_SINGLE = "SINGLE";
    private static final String RULE_CATEGORY = "CATEGORY";
    private static final String CATEGORY_GENTLE = "GENTLE_HYDRATOR";

    private final IngredientDictMapper dictMapper;
    private final IngredientConflictRuleMapper ruleMapper;
    private final IngredientAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    private final Map<String, IngredientDictEntity> keywordToDict = new ConcurrentHashMap<>();
    private final Map<String, IngredientConflictRuleEntity> singleRuleMap = new ConcurrentHashMap<>();
    private final Map<String, IngredientConflictRuleEntity> categoryRuleMap = new ConcurrentHashMap<>();

    public IngredientCheckServiceImpl(
            IngredientDictMapper dictMapper,
            IngredientConflictRuleMapper ruleMapper,
            IngredientAuditLogMapper auditLogMapper,
            ObjectMapper objectMapper) {
        this.dictMapper = dictMapper;
        this.ruleMapper = ruleMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void reloadRules() {
        keywordToDict.clear();
        singleRuleMap.clear();
        categoryRuleMap.clear();

        List<IngredientDictEntity> dicts = dictMapper.selectList(null);
        for (IngredientDictEntity dict : dicts) {
            registerKeyword(dict.getInciNameCn(), dict);
            registerKeyword(dict.getInciNameEn(), dict);
            if (StringUtils.hasText(dict.getAliasNames())) {
                try {
                    List<String> aliases = objectMapper.readValue(dict.getAliasNames(), new TypeReference<>() {});
                    for (String alias : aliases) {
                        registerKeyword(alias, dict);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse alias_names for {}: {}", dict.getInciNameCn(), ex.getMessage());
                }
            }
        }

        List<IngredientConflictRuleEntity> rules = ruleMapper.selectList(null);
        for (IngredientConflictRuleEntity rule : rules) {
            if (RULE_SINGLE.equals(rule.getRuleLevel())
                    && StringUtils.hasText(rule.getIngredientA())
                    && StringUtils.hasText(rule.getIngredientB())) {
                singleRuleMap.put(pairKey(rule.getIngredientA(), rule.getIngredientB()), rule);
            } else if (RULE_CATEGORY.equals(rule.getRuleLevel())
                    && StringUtils.hasText(rule.getCategoryA())
                    && StringUtils.hasText(rule.getCategoryB())) {
                categoryRuleMap.put(pairKey(rule.getCategoryA(), rule.getCategoryB()), rule);
            }
        }
        log.info("Loaded {} ingredient keywords, {} SINGLE rules, {} CATEGORY rules",
                keywordToDict.size(), singleRuleMap.size(), categoryRuleMap.size());
    }

    @Override
    public IngredientCheckResponse check(String userId, String sessionId, String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        Map<String, IngredientDictEntity> matchedMap = new LinkedHashMap<>();
        for (Map.Entry<String, IngredientDictEntity> entry : keywordToDict.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                IngredientDictEntity dict = entry.getValue();
                matchedMap.putIfAbsent(dict.getInciNameCn(), dict);
            }
        }

        List<IngredientDictEntity> matchedList = new ArrayList<>(matchedMap.values());
        List<String> matchedNames = matchedList.stream().map(IngredientDictEntity::getInciNameCn).toList();
        List<ConflictDetail> conflicts = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();

        // L1: 单体精准查表 (P0)
        for (int i = 0; i < matchedList.size(); i++) {
            for (int j = i + 1; j < matchedList.size(); j++) {
                String a = matchedList.get(i).getInciNameCn();
                String b = matchedList.get(j).getInciNameCn();
                String key = pairKey(a, b);
                IngredientConflictRuleEntity rule = singleRuleMap.get(key);
                if (rule != null && seenPairs.add("S:" + key)) {
                    conflicts.add(new ConflictDetail(
                            RULE_SINGLE,
                            rule.getConflictLevel(),
                            a + " + " + b,
                            rule.getRiskMechanism(),
                            rule.getHandlingAdvice()));
                }
            }
        }

        // L2: 大类与 pH 理化推断 (P1)
        for (int i = 0; i < matchedList.size(); i++) {
            for (int j = i + 1; j < matchedList.size(); j++) {
                IngredientDictEntity d1 = matchedList.get(i);
                IngredientDictEntity d2 = matchedList.get(j);
                String catKey = pairKey(d1.getCategory(), d2.getCategory());
                IngredientConflictRuleEntity rule = categoryRuleMap.get(catKey);
                if (rule != null && seenPairs.add("C:" + catKey)) {
                    conflicts.add(new ConflictDetail(
                            RULE_CATEGORY,
                            rule.getConflictLevel(),
                            d1.getInciNameCn() + "(" + d1.getCategory() + ") + " + d2.getInciNameCn() + "(" + d2.getCategory() + ")",
                            rule.getRiskMechanism(),
                            rule.getHandlingAdvice()));
                }
            }
        }

        // L3 & L4: 存在 >=2 个非温和活性成分且未命中 L1/L2 规则 -> 保守提示 + 异步审计
        List<IngredientDictEntity> actives = matchedList.stream()
                .filter(d -> !CATEGORY_GENTLE.equals(d.getCategory()))
                .toList();
        boolean requiresGuard = conflicts.isEmpty() && actives.size() >= 2;
        String guardNotice = requiresGuard
                ? "检测到多种高活性成分同时出现，暂无明确配伍禁忌记录，建议先耳后点涂测试或早晚错峰使用。"
                : null;

        if (requiresGuard) {
            logAuditAsync(userId, sessionId, actives);
        }

        return new IngredientCheckResponse(matchedNames, conflicts, requiresGuard, guardNotice);
    }

    private void logAuditAsync(String userId, String sessionId, List<IngredientDictEntity> actives) {
        CompletableFuture.runAsync(() -> {
            try {
                List<String> names = actives.stream().map(IngredientDictEntity::getInciNameCn).toList();
                List<String> cats = actives.stream().map(IngredientDictEntity::getCategory).distinct().toList();

                IngredientAuditLogEntity entity = new IngredientAuditLogEntity();
                entity.setUserId(userId);
                entity.setSessionId(StringUtils.hasText(sessionId) ? sessionId : "direct_check");
                entity.setIngredientNames(objectMapper.writeValueAsString(names));
                entity.setCategoryTags(objectMapper.writeValueAsString(cats));
                entity.setAuditStatus("PENDING");
                auditLogMapper.insert(entity);
            } catch (Exception ex) {
                log.warn("Async ingredient audit log failed: userId={}", userId, ex);
            }
        });
    }

    private void registerKeyword(String keyword, IngredientDictEntity dict) {
        if (StringUtils.hasText(keyword)) {
            keywordToDict.put(keyword.trim().toLowerCase(Locale.ROOT), dict);
        }
    }

    private String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "::" + b : b + "::" + a;
    }
}
