package com.owl.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class TenantAuth {
    @Value("${owl.security.enabled:false}")
    private boolean securityEnabled;

    public String tenantFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            Object t = jwt.getClaims().get("tenant");
            if (t == null) t = jwt.getClaims().get("tenant_id");
            return t == null ? null : t.toString();
        }
        return null;
    }

    public void authorize(String tenantId) {
        if (!securityEnabled) return;
        String fromJwt = tenantFromAuth();
        if (fromJwt == null) return; // let policy handle auth; no explicit tenant claim
        if (!fromJwt.equals(tenantId)) {
            throw new SecurityException("Forbidden for tenant: " + tenantId);
        }
    }
}

