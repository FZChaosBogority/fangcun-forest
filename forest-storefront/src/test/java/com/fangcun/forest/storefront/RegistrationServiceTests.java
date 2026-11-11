package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.mapper.StoreUserMapper;
import com.fangcun.forest.storefront.service.RegistrationService;
import com.fangcun.forest.storefront.web.RegistrationForm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTests {
    private final StoreUserMapper users = mock(StoreUserMapper.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final RegistrationService service = new RegistrationService(users, encoder);
    private RegistrationForm form() {
        RegistrationForm form = new RegistrationForm(); form.setUsername("new_forest");
        form.setDisplayName("新旅人"); form.setPassword("Green2026!"); form.setConfirmPassword("Green2026!"); return form;
    }
    @Test
    void registrationStoresOnlyAHashAndAlwaysCreatesAnEnabledCustomer() {
        service.register(form());
        var saved = ArgumentCaptor.forClass(StoreUser.class); verify(users).insert(saved.capture());
        assertEquals(UserRole.CUSTOMER, saved.getValue().getRole());
        assertEquals(AccountStatus.ENABLED, saved.getValue().getStatus());
        assertNotEquals("Green2026!", saved.getValue().getPasswordHash());
        assertTrue(encoder.matches("Green2026!", saved.getValue().getPasswordHash()));
    }
    @Test
    void duplicateUsernameBecomesAFriendlyConflict() {
        when(users.insert(any(StoreUser.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(BusinessException.class, () -> service.register(form()));
    }
    @Test
    void mismatchedOrOverlongUtf8PasswordsNeverReachTheDatabase() {
        RegistrationForm form = form(); form.setConfirmPassword("Other2026!");
        assertThrows(BusinessException.class, () -> service.register(form));
        form.setPassword("森".repeat(25)); form.setConfirmPassword(form.getPassword());
        assertThrows(BusinessException.class, () -> service.register(form));
        verifyNoInteractions(users);
    }
}
