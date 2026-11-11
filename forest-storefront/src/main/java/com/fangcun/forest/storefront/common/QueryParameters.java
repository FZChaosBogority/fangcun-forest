package com.fangcun.forest.storefront.common;

import org.springframework.http.HttpStatus;

public final class QueryParameters {
    private QueryParameters() { }
    public static String text(String value, int max, String label) {
        String term = value == null ? "" : value.strip();
        if (term.length() > max) throw new BusinessException(HttpStatus.BAD_REQUEST, label + "不能超过 " + max + " 个字符。");
        return term;
    }
    public static <E extends Enum<E>> E optionalEnum(Class<E> type, String value) {
        if (value == null || value.isBlank() || value.equals("ALL")) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException exception) { throw new BusinessException(HttpStatus.BAD_REQUEST, "状态参数不正确。"); }
    }
}
