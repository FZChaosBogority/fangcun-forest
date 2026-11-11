package com.fangcun.forest.storefront.web;

import com.fangcun.forest.storefront.entity.CartItem;
import com.fangcun.forest.storefront.entity.Product;
import java.math.BigDecimal;
import java.util.List;

public record CartView(List<Line> lines, BigDecimal total, int quantity) {
    public boolean isEmpty() { return lines.isEmpty(); }
    public boolean isCheckoutReady() { return !isEmpty() && lines.stream().allMatch(Line::available); }
    public record Line(CartItem item, Product product, boolean available, String issue) {
        public BigDecimal getAmount() { return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())); }
    }
}
