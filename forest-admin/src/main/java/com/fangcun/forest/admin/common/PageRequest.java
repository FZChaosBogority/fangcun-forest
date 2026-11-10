package com.fangcun.forest.admin.common;

import org.springframework.http.HttpStatus;

/** 页码小于 1 归一为 1；页大小只接受 1—50。 */
public record PageRequest(long page, long size) {

    public PageRequest {
        page = Math.max(1L, page);
        if (size < 1 || size > 50) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "每页数量必须为 1 到 50。");
        }
        // 防止分页偏移量乘法溢出。
        if (page - 1 > Long.MAX_VALUE / size) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "页码超出允许范围。");
        }
    }
}
