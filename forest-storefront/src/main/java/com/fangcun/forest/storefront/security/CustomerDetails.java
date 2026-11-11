package com.fangcun.forest.storefront.security;

import com.fangcun.forest.storefront.entity.StoreUser;
import com.fangcun.forest.storefront.enums.AccountStatus;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class CustomerDetails extends User {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String displayName;
    private final String credentialStamp;

    public CustomerDetails(StoreUser account) {
        super(account.getUsername(), account.getPasswordHash(),
                account.getStatus() == AccountStatus.ENABLED, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
        this.userId = account.getId();
        this.displayName = account.getDisplayName();
        this.credentialStamp = CredentialStamp.of(account.getPasswordHash());
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getCredentialStamp() { return credentialStamp; }
}
