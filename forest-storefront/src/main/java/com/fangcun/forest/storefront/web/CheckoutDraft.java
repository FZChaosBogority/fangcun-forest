package com.fangcun.forest.storefront.web;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Server-side quote; no client-supplied price or amount is accepted. */
public record CheckoutDraft(long userId, String orderNo, Instant createdAt, List<Line> lines)
        implements Serializable {
    public BigDecimal getTotal() {
        return lines.stream().map(Line::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public record Line(long cartId, long productId, int quantity, int cartVersion,
            BigDecimal unitPrice, String name, String image) implements Serializable {
        public BigDecimal getAmount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }
}
