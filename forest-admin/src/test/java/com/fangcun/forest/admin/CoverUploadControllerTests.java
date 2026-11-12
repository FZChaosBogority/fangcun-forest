package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.common.CurrentAdminAdvice;
import com.fangcun.forest.admin.config.AdminSecurityConfig;
import com.fangcun.forest.admin.controller.CoverUploadController;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.AccountStatus;
import com.fangcun.forest.admin.enums.UserRole;
import com.fangcun.forest.admin.security.AdminDetails;
import com.fangcun.forest.admin.service.AdminAccountService;
import com.fangcun.forest.admin.service.CoverImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {AdminSecurityConfig.class, CurrentAdminAdvice.class, CoverUploadController.class})
class CoverUploadControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean AdminAccountService accounts;
    @MockitoBean CoverImageStorage images;
    AdminDetails admin;
    private final MockMultipartFile image = new MockMultipartFile("file", "cover.png", "image/png", new byte[] {1, 2});

    @BeforeEach void prepare() {
        var user = new StoreUser(); user.setId(2L); user.setUsername("forest_admin");
        user.setDisplayName("管理员"); user.setRole(UserRole.ADMIN); user.setStatus(AccountStatus.ENABLED);
        user.setPasswordHash("test-only-credential-stamp"); admin = new AdminDetails(user);
        when(accounts.isCurrentAdmin(eq(2L), anyString())).thenReturn(true);
    }
    @Test void guestAndCustomerCannotUpload() throws Exception {
        mvc.perform(multipart("/media/covers").file(image).with(csrf().asHeader())).andExpect(status().isFound());
        mvc.perform(multipart("/media/covers").file(image).with(user("forest").roles("CUSTOMER")).with(csrf().asHeader())).andExpect(status().isForbidden());
        verifyNoInteractions(images);
    }
    @Test void uploadRequiresCsrfEvenForAdministrators() throws Exception {
        mvc.perform(multipart("/media/covers").file(image).with(user(admin))).andExpect(status().isForbidden());
        verifyNoInteractions(images);
    }
    @Test void authenticatedUploadReturnsOnlyPublicPathAndDimensions() throws Exception {
        when(images.save(any())).thenReturn(new CoverImageStorage.StoredCover("/images/uploads/cover-abc.png", 16, 12));
        mvc.perform(multipart("/media/covers").file(image).with(user(admin)).with(csrf().asHeader()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.path").value("/images/uploads/cover-abc.png"))
                .andExpect(jsonPath("$.width").value(16)).andExpect(jsonPath("$.height").value(12));
    }
    @Test void invalidImageReturnsReadableJsonInsteadOfAnHtmlPage() throws Exception {
        when(images.save(any())).thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "图片内容无效"));
        mvc.perform(multipart("/media/covers").file(image).with(user(admin)).with(csrf().asHeader()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("图片内容无效"));
    }
    @Test void missingFileReturnsJsonAndNeverStoresAnything() throws Exception {
        mvc.perform(multipart("/media/covers").with(user(admin)).with(csrf().asHeader()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(images);
    }
}
