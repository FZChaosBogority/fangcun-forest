package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.common.CurrentAccountAdvice;
import com.fangcun.forest.storefront.config.StorefrontSecurityConfig;
import com.fangcun.forest.storefront.controller.AuthController;
import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import com.fangcun.forest.storefront.enums.UserRole;
import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.service.CustomerAccountService;
import com.fangcun.forest.storefront.service.CaptchaService;
import com.fangcun.forest.storefront.service.RegistrationService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AuthController.class, CurrentAccountAdvice.class,
        StorefrontSecurityConfig.class, AuthFlowTests.PublicCatalog.class})
class AuthFlowTests {

    // Only a public endpoint fixture. These tests do not claim to verify real catalog SQL.
    @RestController
    static class PublicCatalog {
        @GetMapping("/main")
        String main() { return "public catalog"; }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerAccountService accountService;
    @MockitoBean
    private CaptchaService captchas;
    @MockitoBean
    private RegistrationService registrations;

    private StoreUser account;

    @BeforeEach
    void prepareCustomer() {
        // Captcha generation/one-use behavior is covered separately; isolate password tests.
        when(captchas.verify(nullable(HttpSession.class), eq("login"), nullable(String.class))).thenReturn(true);
        account = new StoreUser();
        account.setId(1L);
        account.setUsername("forest");
        account.setDisplayName("森林旅人");
        account.setRole(UserRole.CUSTOMER);
        account.setStatus(AccountStatus.ENABLED);
        account.setPasswordHash(new BCryptPasswordEncoder(4).encode("Forest2026!"));
        when(accountService.loadUserByUsername("forest")).thenAnswer(invocation -> new CustomerDetails(account));
    }

    @Test
    void visitorCanOpenTheWelcomePage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("商城账号登录")))
                .andExpect(content().string(containsString("先逛逛商城")));
    }

    @Test
    void visitorCanBrowseWithoutAuthentication() throws Exception {
        mvc.perform(get("/main")).andExpect(status().isOk()).andExpect(unauthenticated());
    }

    @Test
    void validPasswordCreatesAnAuthenticatedSession() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("username", "forest").param("password", "Forest2026!"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/main"))
                .andExpect(authenticated().withUsername("forest"));
    }

    @Test
    void productLoginReturnsToThatProduct() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "forest").param("password", "Forest2026!")
                        .param("returnTo", "/product?id=2"))
                .andExpect(redirectedUrl("/product?id=2")).andExpect(authenticated().withUsername("forest"));
    }

    @Test
    void cartAndOrderLoginsReturnToTheRequestedPrivatePage() throws Exception {
        for (String destination : new String[] { "/cart", "/orders", "/account" }) {
            mvc.perform(post("/login").with(csrf()).param("username", "forest").param("password", "Forest2026!")
                            .param("returnTo", destination))
                    .andExpect(redirectedUrl(destination)).andExpect(authenticated().withUsername("forest"));
        }
    }

    @Test
    void externalLoginReturnCannotRedirectOutsideTheStore() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "forest").param("password", "Forest2026!")
                        .param("returnTo", "https://example.com/product?id=2"))
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    void failedLoginKeepsTheProductReturnAddress() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "forest").param("password", "incorrect")
                        .param("returnTo", "/product?id=2"))
                .andExpect(redirectedUrl("/login?error&returnTo=%2Fproduct%3Fid%3D2")).andExpect(unauthenticated());
    }

    @Test
    void wrongPasswordDoesNotAuthenticate() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("username", "forest").param("password", "incorrect"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void disabledCustomerCannotLogIn() throws Exception {
        account.setStatus(AccountStatus.DISABLED);
        mvc.perform(post("/login").with(csrf())
                        .param("username", "forest").param("password", "Forest2026!"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void loginWithoutCsrfIsRejectedBeforeAccountLookup() throws Exception {
        mvc.perform(post("/login").param("username", "forest").param("password", "Forest2026!"))
                .andExpect(status().isForbidden()).andExpect(unauthenticated());
        verify(accountService, never()).loadUserByUsername(anyString());
    }

    @Test
    void databaseFailureIsDifferentFromIncorrectCredentials() throws Exception {
        when(accountService.loadUserByUsername("forest"))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        mvc.perform(post("/login").with(csrf())
                        .param("username", "forest").param("password", "Forest2026!"))
                .andExpect(redirectedUrl("/login?unavailable")).andExpect(unauthenticated());
    }

    @Test
    void disablingAnAlreadyLoggedInCustomerInvalidatesAccess() throws Exception {
        when(accountService.isCurrentCustomer(eq(1L), anyString())).thenReturn(false);
        mvc.perform(get("/main").with(user(new CustomerDetails(account))))
                .andExpect(redirectedUrl("/login?expired")).andExpect(unauthenticated());
    }

    @Test
    void invalidCaptchaStopsBeforePasswordLookup() throws Exception {
        when(captchas.verify(nullable(HttpSession.class), eq("login"), nullable(String.class))).thenReturn(false);
        mvc.perform(post("/login").with(csrf()).param("username", "forest").param("password", "Forest2026!"))
                .andExpect(redirectedUrl("/login?captcha")).andExpect(unauthenticated());
        verify(accountService, never()).loadUserByUsername(anyString());
    }

    @Test
    void failedCaptchaAlsoKeepsTheProductReturnAddress() throws Exception {
        when(captchas.verify(nullable(HttpSession.class), eq("login"), nullable(String.class))).thenReturn(false);
        mvc.perform(post("/login").with(csrf()).param("returnTo", "/product?id=1"))
                .andExpect(redirectedUrl("/login?captcha&returnTo=%2Fproduct%3Fid%3D1"));
    }

    @Test
    void guestCannotMutateACartWithoutLoggingIn() throws Exception {
        mvc.perform(post("/cart/add").with(csrf()).param("productId", "1"))
                .andExpect(status().isFound()).andExpect(unauthenticated());
    }

    @Test
    void logoutClearsAuthentication() throws Exception {
        mvc.perform(post("/logout").with(user(new CustomerDetails(account))).with(csrf()))
                .andExpect(redirectedUrl("/main")).andExpect(unauthenticated());
    }

    @Test
    void confirmedAsyncLogoutClearsAuthenticationWithoutRedirecting() throws Exception {
        mvc.perform(post("/logout").accept("application/json").with(user(new CustomerDetails(account))).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Location"))
                .andExpect(unauthenticated());
    }

    @Test
    void nativeProductLogoutStaysOnTheProduct() throws Exception {
        mvc.perform(post("/logout").param("returnTo", "/product?id=1")
                        .with(user(new CustomerDetails(account))).with(csrf()))
                .andExpect(redirectedUrl("/product?id=1")).andExpect(unauthenticated());
    }

    @Test
    void logoutStillRequiresCsrf() throws Exception {
        mvc.perform(post("/logout").accept("application/json").with(user(new CustomerDetails(account))))
                .andExpect(status().isForbidden());
    }
}
