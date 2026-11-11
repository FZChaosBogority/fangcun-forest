package com.fangcun.forest.storefront.service;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.mapper.StoreUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** All cart/order writes lock this row first, including empty-cart insertions. */
@Component
public class CustomerLock {
    private final StoreUserMapper users;
    public CustomerLock(StoreUserMapper users) { this.users = users; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(long userId) {
        StoreUser user = users.lockById(userId);
        if (user == null || user.getRole() != UserRole.CUSTOMER || user.getStatus() != AccountStatus.ENABLED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号不可用，请重新登录。");
        }
    }
}
