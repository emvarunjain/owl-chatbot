package com.owl.security;

import com.owl.model.ApiToken;
import com.owl.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void sets_auth_when_valid_api_key() throws ServletException, IOException {
        ApiTokenService svc = mock(ApiTokenService.class);
        ApiKeyAuthFilter f = new ApiKeyAuthFilter(svc);
        ApiToken t = new ApiToken();
        t.setName("robot"); t.setScopes(List.of("admin:read"));
        when(svc.verify("abc")).thenReturn(Optional.of(t));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/metrics");
        req.addHeader("X-API-Key", "abc");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { /* pass */ };
        f.doFilter(req, resp, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("SCOPE_admin:read")));
    }
}

