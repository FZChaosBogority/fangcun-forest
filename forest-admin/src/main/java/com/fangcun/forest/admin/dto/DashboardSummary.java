package com.fangcun.forest.admin.dto;

public record DashboardSummary(long categories, long products, long onSale, long orders,
        long customers, long awaitingShipment, long lowStock, java.math.BigDecimal paidAmount) {
}
