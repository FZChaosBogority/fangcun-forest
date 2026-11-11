package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.security.AdminDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuthController {
    @GetMapping("/login")
    public String login(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminDetails ? "redirect:/" : "auth/login";
    }
}
