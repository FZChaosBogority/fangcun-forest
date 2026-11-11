package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.security.LoginNavigation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginNavigationTests {
    @Test
    void onlyKnownStorefrontViewsCanBeReturnTargets() {
        assertEquals("/product?id=42", LoginNavigation.destination("/product?id=42"));
        for (String page : new String[] { "/cart", "/orders", "/account" }) {
            assertEquals(page, LoginNavigation.destination(page));
            assertEquals("/main", LoginNavigation.publicDestination(page));
        }
        for (String target : new String[] { "https://example.com", "//example.com", "/\\example.com",
                "/product?id=1&next=https://example.com", "/product?id=0", "/product?id=-1",
                "/orders/1/pay", "/logout", "/product?id=9999999999999999999", "/product?id=1\r\nLocation: //example.com" }) {
            assertEquals("/main", LoginNavigation.destination(target), target);
        }
        assertEquals("/main", LoginNavigation.destination(null));
    }
}
