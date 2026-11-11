package com.fangcun.forest.admin.security;

import com.fangcun.forest.admin.common.Revision;
import com.fangcun.forest.admin.entity.StoreUser;
import com.fangcun.forest.admin.enums.AccountStatus;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class AdminDetails extends User {
    private static final long serialVersionUID = 1L;
    private final Long userId;
    private final String displayName;
    private final String credentialStamp;
    public AdminDetails(StoreUser user) {
        super(user.getUsername(), user.getPasswordHash(), user.getStatus() == AccountStatus.ENABLED,
                true, true, true, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        userId = user.getId(); displayName = user.getDisplayName();
        credentialStamp = Revision.of(user.getPasswordHash());
    }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getCredentialStamp() { return credentialStamp; }
}
