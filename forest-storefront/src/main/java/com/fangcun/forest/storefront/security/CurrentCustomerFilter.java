package com.fangcun.forest.storefront.security;

import com.fangcun.forest.storefront.service.CustomerAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Recheck account status so a disabled account cannot keep using an old session. */
public class CurrentCustomerFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CurrentCustomerFilter.class);
    private final CustomerAccountService accountService;

    public CurrentCustomerFilter(CustomerAccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomerDetails customer) {
            boolean active;
            try {
                active = accountService.isCurrentCustomer(customer.getUserId(), customer.getCredentialStamp());
            } catch (DataAccessException exception) {
                log.error("Cannot verify current customer state", exception);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            if (!active) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) { session.invalidate(); }
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
