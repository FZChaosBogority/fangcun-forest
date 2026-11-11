package com.fangcun.forest.storefront.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.mapper.StoreUserMapper;
import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.security.CredentialStamp;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerAccountService implements UserDetailsService {

    private final StoreUserMapper userMapper;

    public CustomerAccountService(StoreUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalized = username == null ? "" : username.strip();
        if (normalized.isEmpty() || normalized.length() > 50) {
            throw new UsernameNotFoundException("Invalid customer account");
        }
        StoreUser account = userMapper.selectOne(new LambdaQueryWrapper<StoreUser>()
                .eq(StoreUser::getUsername, normalized));
        if (account == null || account.getRole() != UserRole.CUSTOMER) {
            throw new UsernameNotFoundException("Invalid customer account");
        }
        return new CustomerDetails(account);
    }

    public boolean isActiveCustomer(long userId) {
        StoreUser account = userMapper.selectById(userId);
        return account != null && account.getRole() == UserRole.CUSTOMER
                && account.getStatus() == AccountStatus.ENABLED;
    }

    public boolean isCurrentCustomer(long userId, String credentialStamp) {
        StoreUser account = userMapper.selectById(userId);
        return account != null && account.getRole() == UserRole.CUSTOMER && account.getStatus() == AccountStatus.ENABLED
                && CredentialStamp.of(account.getPasswordHash()).equals(credentialStamp);
    }
}
