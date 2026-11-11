package com.fangcun.forest.storefront.common;

import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentAccountAdvice {
    private final CartService carts;
    public CurrentAccountAdvice(CartService carts) { this.carts = carts; }

    @ModelAttribute("cartQuantity")
    public int cartQuantity(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof CustomerDetails customer
                ? carts.quantity(customer.getUserId()) : 0;
    }

    /** Expose only safe display fields, never the UserDetails password property. */
    public record AccountSummary(Long id, String username, String displayName) { }

    @ModelAttribute("currentAccount")
    public AccountSummary currentAccount(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomerDetails customer) {
            return new AccountSummary(customer.getUserId(), customer.getUsername(), customer.getDisplayName());
        }
        return null;
    }
}
