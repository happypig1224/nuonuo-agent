package com.nuonuo.ingredient.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ingredient_dict")
public class IngredientDictEntity {

    private Long id;
    private String inciNameCn;
    private String inciNameEn;
    private String aliasNames;
    private String category;
    private BigDecimal phMin;
    private BigDecimal phMax;
    private Integer irritancyLevel;
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

    public String getInciNameCn() {
        return inciNameCn;
    }

    public void setInciNameCn(String inciNameCn) {
        this.inciNameCn = inciNameCn;
    }

    public String getInciNameEn() {
        return inciNameEn;
    }

    public void setInciNameEn(String inciNameEn) {
        this.inciNameEn = inciNameEn;
    }

    public String getAliasNames() {
        return aliasNames;
    }

    public void setAliasNames(String aliasNames) {
        this.aliasNames = aliasNames;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPhMin() {
        return phMin;
    }

    public void setPhMin(BigDecimal phMin) {
        this.phMin = phMin;
    }

    public BigDecimal getPhMax() {
        return phMax;
    }

    public void setPhMax(BigDecimal phMax) {
        this.phMax = phMax;
    }

    public Integer getIrritancyLevel() {
        return irritancyLevel;
    }

    public void setIrritancyLevel(Integer irritancyLevel) {
        this.irritancyLevel = irritancyLevel;
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
