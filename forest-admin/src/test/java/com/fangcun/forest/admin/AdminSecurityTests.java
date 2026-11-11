package com.fangcun.forest.admin;

import com.fangcun.forest.admin.config.AdminSecurityConfig;
import com.fangcun.forest.admin.controller.AdminAuthController;
import com.fangcun.forest.admin.common.CurrentAdminAdvice;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.*;
import com.fangcun.forest.admin.security.AdminDetails;
import com.fangcun.forest.admin.service.AdminAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {AdminSecurityConfig.class, AdminAuthController.class, CurrentAdminAdvice.class, AdminSecurityTests.ManagementFixture.class})
class AdminSecurityTests {
    @RestController static class ManagementFixture {
        @GetMapping("/") String home() { return "workspace"; }
        @PostMapping("/products/new") String create() { return "created"; }
    }
    @Autowired MockMvc mvc;
    @MockitoBean AdminAccountService accounts;
    StoreUser admin;
    @BeforeEach void prepare() {
        admin = new StoreUser(); admin.setId(2L); admin.setUsername("forest_admin"); admin.setDisplayName("管理员");
        admin.setRole(UserRole.ADMIN); admin.setStatus(AccountStatus.ENABLED);
        admin.setPasswordHash(new BCryptPasswordEncoder(4).encode("Forest2026!"));
        when(accounts.loadUserByUsername("forest_admin")).thenAnswer(call -> new AdminDetails(admin));
        when(accounts.isCurrentAdmin(eq(2L), anyString())).thenReturn(true);
    }
    @Test void guestMustLogInToSeeTheDashboard() throws Exception { mvc.perform(get("/")).andExpect(status().isFound()).andExpect(unauthenticated()); }
    @Test void customerCannotUseAdminMutations() throws Exception {
        mvc.perform(post("/products/new").with(user("forest").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void administratorCanAuthenticate() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "forest_admin").param("password", "Forest2026!"))
            .andExpect(redirectedUrl("/")).andExpect(authenticated().withRoles("ADMIN"));
    }
    @Test void adminMutationStillRequiresCsrf() throws Exception {
        mvc.perform(post("/products/new").with(user(new AdminDetails(admin)))).andExpect(status().isForbidden());
    }
    @Test void validAdminMutationIsAllowed() throws Exception {
        mvc.perform(post("/products/new").with(user(new AdminDetails(admin))).with(csrf())).andExpect(status().isOk());
    }
    @Test void invalidatedCredentialStampEndsTheOldSession() throws Exception {
        when(accounts.isCurrentAdmin(eq(2L), anyString())).thenReturn(false);
        mvc.perform(get("/").with(user(new AdminDetails(admin)))).andExpect(redirectedUrl("/login?expired")).andExpect(unauthenticated());
    }
}
