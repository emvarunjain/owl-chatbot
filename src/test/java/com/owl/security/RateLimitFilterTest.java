package com.owl.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    @Test
    void rate_limits_by_tenant() throws ServletException, IOException {
        RateLimitFilter f = new RateLimitFilter(2, new SimpleMeterRegistry(), new TenantAuth());

        FilterChain chain = (req, resp) -> { /* pass */ };

        MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/api/v1/chat");
        r1.setParameter("tenantId", "acme");
        MockHttpServletResponse s1 = new MockHttpServletResponse();
        f.doFilter(r1, s1, chain);
        assertEquals(200, s1.getStatus()); // default OK

        MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/api/v1/chat");
        r2.setParameter("tenantId", "acme");
        MockHttpServletResponse s2 = new MockHttpServletResponse();
        f.doFilter(r2, s2, chain);
        assertEquals(200, s2.getStatus());

        MockHttpServletRequest r3 = new MockHttpServletRequest("POST", "/api/v1/chat");
        r3.setParameter("tenantId", "acme");
        MockHttpServletResponse s3 = new MockHttpServletResponse();
        f.doFilter(r3, s3, chain);
        assertEquals(429, s3.getStatus());
    }

    @Test
    void excludes_actuator() throws ServletException, IOException {
        RateLimitFilter f = new RateLimitFilter(1, new SimpleMeterRegistry(), new TenantAuth());
        FilterChain chain = (req, resp) -> { /* pass */ };
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse s = new MockHttpServletResponse();
        f.doFilter(r, s, chain);
        assertEquals(200, s.getStatus());
    }
}

