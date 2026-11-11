package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangcun.forest.admin.common.Revision;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.AccountStatus;
import com.fangcun.forest.admin.enums.UserRole;
import com.fangcun.forest.admin.mapper.StoreUserMapper;
import com.fangcun.forest.admin.security.AdminDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAccountService implements UserDetailsService {
    private final StoreUserMapper users;
    public AdminAccountService(StoreUserMapper users) { this.users = users; }
    @Override
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank() || username.length() > 50) throw invalid();
        StoreUser user = users.selectOne(new LambdaQueryWrapper<StoreUser>().eq(StoreUser::getUsername, username.strip()));
        if (user == null || user.getRole() != UserRole.ADMIN) throw invalid();
        return new AdminDetails(user);
    }
    public boolean isCurrentAdmin(long id, String stamp) {
        StoreUser user = users.selectById(id);
        return user != null && user.getRole() == UserRole.ADMIN && user.getStatus() == AccountStatus.ENABLED
                && Revision.of(user.getPasswordHash()).equals(stamp);
    }
    private UsernameNotFoundException invalid() { return new UsernameNotFoundException("Invalid administrator account"); }
}
