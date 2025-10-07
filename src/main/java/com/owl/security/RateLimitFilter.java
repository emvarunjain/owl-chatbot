package com.owl.security;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int qpm;
    private final MeterRegistry metrics;
    private final TenantAuth tenantAuth;

    private static class Bucket { volatile long windowStart; AtomicInteger count = new AtomicInteger(); }
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${owl.rateLimit.qpm:120}") int qpm,
                           MeterRegistry metrics,
                           TenantAuth tenantAuth) {
        this.qpm = qpm;
        this.metrics = metrics;
        this.tenantAuth = tenantAuth;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/static") || path.startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenant = request.getParameter("tenantId");
        if (tenant == null) tenant = request.getHeader("X-Tenant-Id");
        if (tenant == null) tenant = tenantAuth.tenantFromAuth();
        if (tenant == null) {
            filterChain.doFilter(request, response);
            return;
        }
        long now = Instant.now().getEpochSecond();
        long window = now / 60L;
        String key = tenant + ":" + window;
        Bucket b = buckets.computeIfAbsent(key, k -> {
            Bucket nb = new Bucket(); nb.windowStart = window; return nb;
        });
        if (b.windowStart != window) {
            b.windowStart = window; b.count.set(0);
        }
        int current = b.count.incrementAndGet();
        if (current > qpm) {
            metrics.counter("rate.limit.exceeded", "tenantId", tenant).increment();
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limited\",\"message\":\"Too many requests\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

