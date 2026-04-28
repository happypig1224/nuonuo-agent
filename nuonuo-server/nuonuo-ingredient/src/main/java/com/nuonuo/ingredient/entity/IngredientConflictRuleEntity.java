package com.nuonuo.ingredient.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ingredient_conflict_rule")
public class IngredientConflictRuleEntity {

    private Long id;
    private String ruleLevel;
    private String ingredientA;
    private String ingredientB;
    private String categoryA;
    private String categoryB;
    private String conflictLevel;
    private String riskMechanism;
    private String handlingAdvice;
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleLevel() {
        return ruleLevel;
    }

    public void setRuleLevel(String ruleLevel) {
        this.ruleLevel = ruleLevel;
    }

    public String getIngredientA() {
        return ingredientA;
    }

    public void setIngredientA(String ingredientA) {
        this.ingredientA = ingredientA;
    }

    public String getIngredientB() {
        return ingredientB;
    }

    public void setIngredientB(String ingredientB) {
        this.ingredientB = ingredientB;
    }

    public String getCategoryA() {
        return categoryA;
    }

    public void setCategoryA(String categoryA) {
        this.categoryA = categoryA;
    }

    public String getCategoryB() {
        return categoryB;
    }

    public void setCategoryB(String categoryB) {
        this.categoryB = categoryB;
    }

    public String getConflictLevel() {
        return conflictLevel;
    }

    public void setConflictLevel(String conflictLevel) {
        this.conflictLevel = conflictLevel;
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

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }
}
