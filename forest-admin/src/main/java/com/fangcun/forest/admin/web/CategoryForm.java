package com.fangcun.forest.admin.web;

import jakarta.validation.constraints.*;
import com.fangcun.forest.admin.entity.Category;

public class CategoryForm {
    @NotBlank(message = "请填写分类编码")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}", message = "分类编码为 2—40 位大写字母、数字或下划线")
    private String code;
    @NotBlank(message = "请填写分类名称")
    @Size(max = 80, message = "分类名称最多 80 个字符")
    private String name;
    private Long parentId;
    @NotNull(message = "请填写排序")
    @Min(value = 0, message = "排序不能小于零")
    @Max(1000000)
    private Integer sortOrder = 0;
    private String revision = "";

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long value) { parentId = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public String getRevision() { return revision; }
    public void setRevision(String value) { revision = value; }
    public static CategoryForm from(Category category, String revision) {
        CategoryForm form = new CategoryForm(); form.setCode(category.getCode()); form.setName(category.getName());
        form.setParentId(category.getParentId()); form.setSortOrder(category.getSortOrder()); form.setRevision(revision);
        return form;
    }
}
