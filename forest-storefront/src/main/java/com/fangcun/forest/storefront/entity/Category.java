package com.fangcun.forest.storefront.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("category")
public class Category extends BaseEntity {

    private String code;

    private Long parentId;

    private String name;

    private Integer sortOrder;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
