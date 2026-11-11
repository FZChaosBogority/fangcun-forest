package com.fangcun.forest.admin.web;

import jakarta.validation.constraints.*;

public class PasswordForm {
    @NotBlank(message = "请填写新密码")
    @Size(min = 8, max = 64, message = "新密码须为 8—64 个字符")
    private String password;
    @NotBlank(message = "请再次输入新密码")
    @Size(max = 64)
    private String confirmPassword;
    @NotBlank
    private String revision;

    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
    public String getRevision() { return revision; }
    public void setRevision(String value) { revision = value; }
    public void clearSecrets() { password = null; confirmPassword = null; }
}
