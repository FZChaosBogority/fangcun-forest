package com.fangcun.forest.admin.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.fangcun.forest.admin.entity.Product;

public class ProductForm {
    @NotBlank(message = "请填写 SKU")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{1,39}", message = "SKU 为 2—40 位字母、数字、下划线或横线")
    private String sku;
    @NotNull(message = "请选择二级分类")
    private Long categoryId;
    @NotBlank(message = "请填写商品名称")
    @Size(max = 120)
    private String name;
    @Size(max = 500, message = "简介最多 500 个字符")
    private String summary;
    @Size(max = 16000, message = "详情最多 16000 个字符")
    private String description;
    @NotBlank(message = "请填写封面图片路径")
    @Size(max = 255)
    private String coverImage = "/images/terrarium.svg";
    @Size(max = 2200, message = "相册最多 8 个本地图片路径")
    private String images = "";
    @NotNull(message = "请填写价格")
    @DecimalMin(value = "0.01", message = "价格须大于零")
    @Digits(integer = 10, fraction = 2, message = "价格最多 10 位整数和 2 位小数")
    private BigDecimal price;
    @NotNull
    @Min(0)
    @Max(1000000)
    private Integer sortOrder = 0;
    @NotNull
    @Min(0)
    @Max(2000000000)
    private Integer initialStock = 0;
    @NotNull
    @Min(0)
    private Integer version = 0;

    public String getSku() { return sku; }
    public void setSku(String value) { sku = value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String value) { coverImage = value; }
    public String getImages() { return images; }
    public void setImages(String value) { images = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public Integer getInitialStock() { return initialStock; }
    public void setInitialStock(Integer value) { initialStock = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public static ProductForm from(Product product, String images) {
        ProductForm form = new ProductForm(); form.setSku(product.getSku()); form.setCategoryId(product.getCategoryId());
        form.setName(product.getName()); form.setSummary(product.getSummary()); form.setDescription(product.getDescription());
        form.setCoverImage(product.getCoverImage()); form.setImages(images); form.setPrice(product.getPrice());
        form.setSortOrder(product.getSortOrder()); form.setVersion(product.getVersion()); return form;
    }
}
