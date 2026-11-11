package com.fangcun.forest.admin.web;

import jakarta.validation.constraints.*;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.AccountStatus;

public class UserForm {
    @NotBlank(message = "请填写昵称")
    @Size(max = 50)
    private String displayName;
    @Email(message = "邮箱格式不正确")
    @Size(max = 120)
    private String email;
    @Pattern(regexp = "^$|[0-9+() -]{6,20}", message = "联系电话格式不正确")
    private String phone;
    @NotNull(message = "请选择账号状态")
    private AccountStatus status = AccountStatus.ENABLED;
    @NotBlank
    private String revision;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus value) { status = value; }
    public String getRevision() { return revision; }
    public void setRevision(String value) { revision = value; }
    public static UserForm from(StoreUser user, String revision) {
        UserForm form = new UserForm(); form.setDisplayName(user.getDisplayName()); form.setEmail(user.getEmail());
        form.setPhone(user.getPhone()); form.setStatus(user.getStatus()); form.setRevision(revision); return form;
    }
}
