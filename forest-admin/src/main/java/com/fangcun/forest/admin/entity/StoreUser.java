package com.fangcun.forest.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fangcun.forest.admin.enums.AccountStatus;
import com.fangcun.forest.admin.enums.UserRole;

@TableName("store_user")
public class StoreUser extends BaseEntity {

    private String username;

    private String passwordHash;

    private String displayName;

    private UserRole role;

    private AccountStatus status;

    private String email;

    private String phone;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
