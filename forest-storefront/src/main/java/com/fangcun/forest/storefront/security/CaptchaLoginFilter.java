package com.fangcun.forest.storefront.security;

import com.fangcun.forest.storefront.service.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class CaptchaLoginFilter extends OncePerRequestFilter {
    private final CaptchaService captchas;
    public CaptchaLoginFilter(CaptchaService captchas) { this.captchas = captchas; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/login".equals(request.getServletPath());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!captchas.verify(request.getSession(false), "login", request.getParameter("captcha"))) {
            response.sendRedirect(request.getContextPath()
                    + LoginNavigation.loginPage("captcha", request.getParameter("returnTo")));
            return;
        }
        chain.doFilter(request, response);
    }
}
