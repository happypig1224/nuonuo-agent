package com.nuonuo.ingredient.dto;

import java.util.List;

/**
 * 成分配伍禁忌四层防御校验响应 DTO
 */
public class IngredientCheckResponse {

    private List<String> matchedIngredients;
    private List<ConflictDetail> conflicts;
    private boolean requiresConservativeGuard;
    private String guardNotice;

    public IngredientCheckResponse() {
    }

    public IngredientCheckResponse(
            List<String> matchedIngredients,
            List<ConflictDetail> conflicts,
            boolean requiresConservativeGuard,
            String guardNotice) {
        this.matchedIngredients = matchedIngredients;
        this.conflicts = conflicts;
        this.requiresConservativeGuard = requiresConservativeGuard;
        this.guardNotice = guardNotice;
    }

    public List<String> getMatchedIngredients() {
        return matchedIngredients;
    }

    public void setMatchedIngredients(List<String> matchedIngredients) {
        this.matchedIngredients = matchedIngredients;
    }

    public List<ConflictDetail> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictDetail> conflicts) {
        this.conflicts = conflicts;
    }

    public boolean isRequiresConservativeGuard() {
        return requiresConservativeGuard;
    }

    public void setRequiresConservativeGuard(boolean requiresConservativeGuard) {
        this.requiresConservativeGuard = requiresConservativeGuard;
    }

    public String getGuardNotice() {
        return guardNotice;
    }

    public void setGuardNotice(String guardNotice) {
        this.guardNotice = guardNotice;
    }

    public static class ConflictDetail {

        private String ruleLevel;
        private String conflictLevel;
        private String pair;
        private String riskMechanism;
        private String handlingAdvice;

        public ConflictDetail() {
        }

        public ConflictDetail(
                String ruleLevel,
                String conflictLevel,
                String pair,
                String riskMechanism,
                String handlingAdvice) {
            this.ruleLevel = ruleLevel;
            this.conflictLevel = conflictLevel;
            this.pair = pair;
            this.riskMechanism = riskMechanism;
            this.handlingAdvice = handlingAdvice;
        }

        public String getRuleLevel() {
            return ruleLevel;
        }

        public void setRuleLevel(String ruleLevel) {
            this.ruleLevel = ruleLevel;
        }

        public String getConflictLevel() {
            return conflictLevel;
        }

        public void setConflictLevel(String conflictLevel) {
            this.conflictLevel = conflictLevel;
        }

        public String getPair() {
            return pair;
        }

        public void setPair(String pair) {
            this.pair = pair;
        }

        public String getRiskMechanism() {
            return riskMechanism;
        }

        public void setRiskMechanism(String riskMechanism) {
            this.riskMechanism = riskMechanism;
        }

        public String getHandlingAdvice() {
            return handlingAdvice;
        }

        public void setHandlingAdvice(String handlingAdvice) {
            this.handlingAdvice = handlingAdvice;
        }
    }
}
