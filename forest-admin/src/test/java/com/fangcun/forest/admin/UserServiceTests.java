package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.*;
import com.fangcun.forest.admin.mapper.StoreUserMapper;
import com.fangcun.forest.admin.service.UserService;
import com.fangcun.forest.admin.web.PasswordForm;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class UserServiceTests {
    final StoreUserMapper users=mock(StoreUserMapper.class);
    final UserService service=new UserService(users,new BCryptPasswordEncoder(4));
    StoreUser user(UserRole role) { StoreUser user=new StoreUser(); user.setId(7L); user.setUsername("forest"); user.setPasswordHash("old-hash"); user.setRole(role); user.setStatus(AccountStatus.ENABLED); return user; }
    PasswordForm form(StoreUser user) { PasswordForm form=new PasswordForm(); form.setPassword("NewForest2026!"); form.setConfirmPassword("NewForest2026!"); form.setRevision(UserService.revision(user)); return form; }
    @Test void administratorCredentialsCannotBeResetThroughCustomerMaintenance() {
        StoreUser user=user(UserRole.ADMIN); when(users.lockById(7)).thenReturn(user);
        assertThrows(BusinessException.class,()->service.resetPassword(7,form(user))); verify(users,never()).update(any(),any());
    }
    @Test void mismatchedPasswordsNeverReachTheDatabase() {
        PasswordForm form=form(user(UserRole.CUSTOMER)); form.setConfirmPassword("different");
        assertThrows(BusinessException.class,()->service.resetPassword(7,form)); verifyNoInteractions(users);
    }
    @Test void staleResetCannotOverwriteANewerPassword() {
        StoreUser user=user(UserRole.CUSTOMER); PasswordForm form=form(user); user.setPasswordHash("newer-hash"); when(users.lockById(7)).thenReturn(user);
        assertThrows(BusinessException.class,()->service.resetPassword(7,form)); verify(users,never()).update(any(),any());
    }
}
