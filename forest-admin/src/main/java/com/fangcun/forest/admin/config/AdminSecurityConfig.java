package com.fangcun.forest.admin.config;

import com.fangcun.forest.admin.security.CurrentAdminFilter;
import com.fangcun.forest.admin.service.AdminAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class AdminSecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean
    public SecurityFilterChain adminSecurity(HttpSecurity http, AdminAccountService accounts) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error", "/actuator/health").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true)
                .failureHandler((request, response, exception) -> response.sendRedirect(request.getContextPath()
                    + (exception instanceof AuthenticationServiceException ? "/login?unavailable" : "/login?error"))))
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("FOREST_ADMIN_SESSION"))
            .requestCache(cache -> cache.disable())
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .addFilterAfter(new CurrentAdminFilter(accounts), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
