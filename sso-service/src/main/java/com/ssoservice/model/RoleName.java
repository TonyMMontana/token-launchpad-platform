package com.ssoservice.model;

import org.springframework.security.core.GrantedAuthority;

public enum RoleName implements GrantedAuthority {
    ADMIN,
    USER,
    VIP_USER,
    ;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
