package com.fangcun.forest.storefront.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

/** Post-login destinations are restricted to known storefront pages. */
public final class LoginNavigation {
    private static final Pattern PRODUCT = Pattern.compile("/product\\?id=([1-9][0-9]{0,18})");
    private static final Set<String> PERSONAL = Set.of("/cart", "/orders", "/account");
    private LoginNavigation() { }

    public static String returnTarget(String value) {
        if (value == null) return "";
        if (PERSONAL.contains(value)) return value;
        Matcher match = PRODUCT.matcher(value);
        if (!match.matches()) return "";
        try { Long.parseLong(match.group(1)); }
        catch (NumberFormatException exception) { return ""; }
        return value;
    }

    public static String destination(String value) {
        String product = returnTarget(value);
        return product.isEmpty() ? "/main" : product;
    }

    public static String publicDestination(String value) {
        String target = returnTarget(value);
        return target.startsWith("/product?") ? target : "/main";
    }

    public static String loginPage(String reason, String value) {
        String product = returnTarget(value);
        return "/login?" + reason + (product.isEmpty() ? "" : "&returnTo="
                + URLEncoder.encode(product, StandardCharsets.UTF_8));
    }
}
