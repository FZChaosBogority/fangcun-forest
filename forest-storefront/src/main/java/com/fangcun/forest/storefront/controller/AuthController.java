package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.security.LoginNavigation;
import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.service.CaptchaService;
import com.fangcun.forest.storefront.service.RegistrationService;
import com.fangcun.forest.storefront.web.RegistrationForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final RegistrationService registrations;
    private final CaptchaService captchas;
    public AuthController(RegistrationService registrations, CaptchaService captchas) {
        this.registrations = registrations; this.captchas = captchas;
    }

    @GetMapping({"/", "/index"})
    public String welcome(@RequestParam(required = false) String returnTo, Model model) {
        model.addAttribute("loginReturnTo", LoginNavigation.returnTarget(returnTo));
        return "auth/welcome";
    }

    @GetMapping("/login")
    public String login(Authentication authentication, @RequestParam(required = false) String returnTo, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomerDetails) {
            return "redirect:" + LoginNavigation.destination(returnTo);
        }
        model.addAttribute("loginReturnTo", LoginNavigation.returnTarget(returnTo));
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Authentication authentication, @RequestParam(required = false) String returnTo, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomerDetails) return "redirect:" + LoginNavigation.destination(returnTo);
        model.addAttribute("loginReturnTo", LoginNavigation.returnTarget(returnTo));
        model.addAttribute("registration", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationForm form,
            BindingResult errors, HttpSession session, Model model, @RequestParam(required = false) String returnTo) {
        model.addAttribute("loginReturnTo", LoginNavigation.returnTarget(returnTo));
        if (!captchas.verify(session, "register", form.getCaptcha())) {
            errors.reject("captcha", "验证码错误或已过期，请使用新图片重试。");
        }
        if (!errors.hasErrors()) {
            try { registrations.register(form); }
            catch (BusinessException exception) { errors.reject("registration", exception.getMessage()); }
        }
        form.clearSecrets();
        if (errors.hasErrors()) return "auth/register";
        return "redirect:" + LoginNavigation.loginPage("registered", returnTo);
    }

    @GetMapping("/account")
    public String account() { return "account/overview"; }
}
