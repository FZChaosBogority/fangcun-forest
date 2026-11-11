package com.fangcun.forest.admin.security;

import com.fangcun.forest.admin.service.AdminAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CurrentAdminFilter extends OncePerRequestFilter {
    private final AdminAccountService accounts;
    public CurrentAdminFilter(AdminAccountService accounts) { this.accounts = accounts; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/actuator/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminDetails admin) {
            boolean current;
            try { current = accounts.isCurrentAdmin(admin.getUserId(), admin.getCredentialStamp()); }
            catch (DataAccessException exception) { response.sendError(503); return; }
            if (!current) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false); if (session != null) session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?expired"); return;
            }
        }
        chain.doFilter(request, response);
    }
}
