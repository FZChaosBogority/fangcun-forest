package com.fangcun.forest.storefront.enums;

public enum ProductSort {
    DEFAULT("推荐排序"), PRICE_ASC("价格从低到高"), PRICE_DESC("价格从高到低"), NEWEST("最新上架");
    private final String label;
    ProductSort(String label) { this.label = label; }
    public String getLabel() { return label; }
}
