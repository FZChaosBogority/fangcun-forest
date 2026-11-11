package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.mapper.StoreUserMapper;
import com.fangcun.forest.storefront.service.CustomerAccountService;
import com.fangcun.forest.storefront.security.CustomerDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerAccountServiceTests {

    private final StoreUserMapper mapper = mock(StoreUserMapper.class);
    private final CustomerAccountService service = new CustomerAccountService(mapper);

    @Test
    void administratorCannotAuthenticateAsAStorefrontCustomer() {
        StoreUser administrator = new StoreUser();
        administrator.setRole(UserRole.ADMIN);
        when(mapper.selectOne(any())).thenReturn(administrator);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("admin"));
    }

    @Test
    void deletedAccountCannotKeepUsingAnExistingSession() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertFalse(service.isActiveCustomer(1L));
    }

    @Test
    void unknownUsernameIsRejected() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }

    @Test
    void passwordResetInvalidatesTheOldSessionEvenAfterCredentialsWereErased() {
        StoreUser user = new StoreUser(); user.setId(1L); user.setUsername("forest"); user.setDisplayName("旅人");
        user.setRole(UserRole.CUSTOMER); user.setStatus(AccountStatus.ENABLED); user.setPasswordHash("old-salted-password-hash");
        CustomerDetails details = new CustomerDetails(user); details.eraseCredentials();
        when(mapper.selectById(1L)).thenReturn(user);
        assertTrue(service.isCurrentCustomer(1L, details.getCredentialStamp()));
        user.setPasswordHash("new-salted-password-hash");
        assertFalse(service.isCurrentCustomer(1L, details.getCredentialStamp()));
    }
}
