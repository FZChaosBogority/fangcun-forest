package com.fangcun.forest.admin.common;

import com.fangcun.forest.admin.security.AdminDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentAdminAdvice {
    public record Summary(Long id, String displayName) { }
    @ModelAttribute("currentAdmin")
    public Summary current(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminDetails admin
                ? new Summary(admin.getUserId(), admin.getDisplayName()) : null;
    }
}
