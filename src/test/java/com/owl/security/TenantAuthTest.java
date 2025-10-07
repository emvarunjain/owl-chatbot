package com.owl.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TenantAuthTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void setEnabled(TenantAuth ta, boolean enabled) throws Exception {
        Field f = TenantAuth.class.getDeclaredField("securityEnabled");
        f.setAccessible(true);
        f.setBoolean(ta, enabled);
    }

    @Test
    void authorize_allows_when_disabled() throws Exception {
        TenantAuth ta = new TenantAuth();
        setEnabled(ta, false);
        assertDoesNotThrow(() -> ta.authorize("acme"));
    }

    @Test
    void authorize_checks_claim_when_enabled() throws Exception {
        TenantAuth ta = new TenantAuth();
        setEnabled(ta, true);
        Jwt jwt = Jwt.withTokenValue("t").header("alg","none").claim("tenant","acme").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        assertDoesNotThrow(() -> ta.authorize("acme"));
        assertThrows(SecurityException.class, () -> ta.authorize("other"));
    }
}

