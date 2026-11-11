package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.admin.aspect.AdminAction;
import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.*;
import com.fangcun.forest.admin.mapper.StoreUserMapper;
import com.fangcun.forest.admin.web.PasswordForm;
import com.fangcun.forest.admin.web.UserForm;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final StoreUserMapper users;
    private final PasswordEncoder encoder;
    public UserService(StoreUserMapper users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }
    public Page<StoreUser> search(String keyword, AccountStatus status, PageRequest page) {
        String term = QueryTerms.escape(keyword, 120);
        var query = new LambdaQueryWrapper<StoreUser>().eq(status != null, StoreUser::getStatus, status);
        if (!term.isEmpty()) query.and(w -> w.like(StoreUser::getUsername, term).or().like(StoreUser::getDisplayName, term).or().like(StoreUser::getEmail, term));
        return users.selectPage(new Page<>(page.page(), page.size()), query.orderByDesc(StoreUser::getCreatedAt, StoreUser::getId));
    }
    public StoreUser get(long id) {
        StoreUser user = users.selectById(id); if (user == null) throw new BusinessException(HttpStatus.NOT_FOUND, "账号不存在。"); return user;
    }
    public static String revision(StoreUser user) {
        return Revision.of(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getStatus(), user.getEmail(), user.getPhone(), user.getPasswordHash(), user.getUpdatedAt());
    }
    @Transactional
    @AdminAction("user.update")
    public void update(long id, UserForm form) {
        StoreUser user = lockCustomer(id, form.getRevision());
        if (users.update(null, new LambdaUpdateWrapper<StoreUser>().eq(StoreUser::getId, user.getId()).eq(StoreUser::getRole, UserRole.CUSTOMER)
            .set(StoreUser::getDisplayName, form.getDisplayName().strip()).set(StoreUser::getEmail, optional(form.getEmail()))
            .set(StoreUser::getPhone, optional(form.getPhone())).set(StoreUser::getStatus, form.getStatus())
            .set(StoreUser::getUpdatedAt, LocalDateTime.now())) != 1) throw conflict("账号已变化，请刷新。");
    }
    @Transactional
    @AdminAction("user.resetPassword")
    public void resetPassword(long id, PasswordForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) throw conflict("两次输入的密码不一致。");
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) throw conflict("新密码的 UTF-8 长度不能超过 72 字节。");
        StoreUser user = lockCustomer(id, form.getRevision());
        if (users.update(null, new LambdaUpdateWrapper<StoreUser>().eq(StoreUser::getId, user.getId()).eq(StoreUser::getRole, UserRole.CUSTOMER)
            .set(StoreUser::getPasswordHash, encoder.encode(form.getPassword())).set(StoreUser::getUpdatedAt, LocalDateTime.now())) != 1) throw conflict("账号已变化，请刷新。");
    }
    private StoreUser lockCustomer(long id, String revision) {
        StoreUser user = users.lockById(id);
        if (user == null) throw new BusinessException(HttpStatus.NOT_FOUND, "账号不存在。");
        if (user.getRole() != UserRole.CUSTOMER) throw conflict("此页面只维护商城用户，管理员账号保持只读。");
        if (!revision(user).equals(revision)) throw conflict("用户资料已变化，请刷新后重试。");
        return user;
    }
    private String optional(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private BusinessException conflict(String text) { return new BusinessException(HttpStatus.CONFLICT, text); }
}
