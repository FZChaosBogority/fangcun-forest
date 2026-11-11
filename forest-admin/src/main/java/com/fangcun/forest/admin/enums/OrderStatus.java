package com.fangcun.forest.admin.enums;

public enum OrderStatus {
    PENDING_PAYMENT("待付款"), PAID("已付款"), SHIPPED("已发货"), COMPLETED("已完成"), CANCELLED("已取消");
    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
