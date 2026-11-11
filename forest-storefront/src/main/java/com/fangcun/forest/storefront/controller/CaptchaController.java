package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.service.CaptchaService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaptchaController {
    private final CaptchaService captchas;
    public CaptchaController(CaptchaService captchas) { this.captchas = captchas; }
    @GetMapping(value = "/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@RequestParam(defaultValue = "login") String purpose,
            HttpSession session) throws IOException {
        if (!"login".equals(purpose) && !"register".equals(purpose)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码用途不正确。");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .contentType(MediaType.IMAGE_PNG).body(captchas.create(session, purpose));
    }
}
