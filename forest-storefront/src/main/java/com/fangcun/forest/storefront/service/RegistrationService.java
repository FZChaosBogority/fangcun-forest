package com.fangcun.forest.storefront.service;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.mapper.StoreUserMapper;
import com.fangcun.forest.storefront.web.RegistrationForm;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final StoreUserMapper users;
    private final PasswordEncoder encoder;
    public RegistrationService(StoreUserMapper users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Transactional
    public void register(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "两次输入的密码不一致。");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密码的 UTF-8 长度不能超过 72 字节，请缩短密码。");
        }
        StoreUser user = new StoreUser();
        user.setUsername(form.getUsername());
        user.setDisplayName(form.getDisplayName().strip());
        user.setPasswordHash(encoder.encode(form.getPassword()));
        user.setEmail(form.getEmail() == null || form.getEmail().isBlank() ? null : form.getEmail().strip());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(AccountStatus.ENABLED);
        try { users.insert(user); }
        catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "这个商城账号已被使用，请换一个。");
        }
    }
}
