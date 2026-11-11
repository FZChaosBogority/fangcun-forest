package com.fangcun.forest.admin.common;

import org.springframework.http.HttpStatus;
public final class QueryTerms {
    private QueryTerms() { }
    public static String escape(String term, int max) {
        String value = term == null ? "" : term.strip();
        if (value.length() > max) throw new BusinessException(HttpStatus.BAD_REQUEST, "搜索条件过长。");
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
