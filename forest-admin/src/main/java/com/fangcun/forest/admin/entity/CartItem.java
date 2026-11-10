package com.fangcun.forest.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

@TableName("cart_item")
public class CartItem extends BaseEntity {

    private Long userId;

    private Long productId;

    private Integer quantity;

    @Version
    private Integer version;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
