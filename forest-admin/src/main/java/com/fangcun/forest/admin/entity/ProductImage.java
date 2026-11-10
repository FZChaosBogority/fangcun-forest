package com.fangcun.forest.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("product_image")
public class ProductImage extends BaseEntity {

    private Long productId;

    private String imagePath;

    private String altText;

    private Integer sortOrder;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
