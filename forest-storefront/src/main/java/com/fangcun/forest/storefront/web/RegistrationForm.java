package com.fangcun.forest.storefront.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @Pattern(regexp = "[A-Za-z0-9_]{3,30}", message = "账号须为 3—30 位英文字母、数字或下划线")
    @NotBlank(message = "请填写商城账号")
    private String username;
    @NotBlank(message = "请填写昵称")
    @Size(max = 50, message = "昵称最多 50 个字符")
    private String displayName;
    @NotBlank(message = "请填写密码")
    @Size(min = 8, max = 64, message = "密码长度须为 8—64 个字符")
    private String password;
    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;
    @Email(message = "邮箱格式不正确")
    @Size(max = 120, message = "邮箱最多 120 个字符")
    private String email;
    @NotBlank(message = "请填写图片验证码")
    private String captcha;
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String value) { captcha = value; }
    public void clearSecrets() { password = null; confirmPassword = null; captcha = null; }
}
