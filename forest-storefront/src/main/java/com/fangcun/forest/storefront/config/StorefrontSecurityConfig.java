package com.fangcun.forest.storefront.config;

import com.fangcun.forest.storefront.security.CurrentCustomerFilter;
import com.fangcun.forest.storefront.security.CaptchaLoginFilter;
import com.fangcun.forest.storefront.security.LoginNavigation;
import com.fangcun.forest.storefront.service.CaptchaService;
import com.fangcun.forest.storefront.service.CustomerAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class StorefrontSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain storefrontSecurity(HttpSecurity http,
            CustomerAccountService accountService, CaptchaService captchas) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/cart", "/cart/**", "/orders", "/orders/**", "/checkout", "/checkout/**", "/account", "/account/**")
                        .hasRole("CUSTOMER")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> response.sendRedirect(
                                request.getContextPath() + LoginNavigation.destination(request.getParameter("returnTo"))))
                        .failureHandler((request, response, exception) -> {
                            String reason = exception instanceof AuthenticationServiceException
                                    ? "unavailable" : "error";
                            response.sendRedirect(request.getContextPath()
                                    + LoginNavigation.loginPage(reason, request.getParameter("returnTo")));
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String accept = request.getHeader("Accept");
                            if (accept != null && accept.contains("application/json")) {
                                response.setStatus(204);
                            } else {
                                response.sendRedirect(request.getContextPath()
                                        + LoginNavigation.publicDestination(request.getParameter("returnTo")));
                            }
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("FOREST_USER_SESSION"))
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(new CaptchaLoginFilter(captchas), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CurrentCustomerFilter(accountService), AnonymousAuthenticationFilter.class);
        // CSRF remains enabled. Thymeleaf adds its token to POST login/logout forms.
        return http.build();
    }
}
