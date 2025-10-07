package com.owl.security;

import com.owl.model.ApiToken;
import com.owl.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final ApiTokenService tokens;

    public ApiKeyAuthFilter(ApiTokenService tokens) { this.tokens = tokens; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("ApiKey ")) apiKey = auth.substring(7).trim();
        }
        if (apiKey != null && !apiKey.isBlank()) {
            Optional<ApiToken> opt = tokens.verify(apiKey);
            if (opt.isPresent()) {
                ApiToken t = opt.get();
                List<GrantedAuthority> auths = t.getScopes().stream()
                        .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
                        .collect(Collectors.toList());
                AbstractAuthenticationToken at = new AbstractAuthenticationToken(auths) {
                    @Override public Object getCredentials() { return ""; }
                    @Override public Object getPrincipal() { return t.getName(); }
                };
                at.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(at);
            }
        }
        filterChain.doFilter(request, response);
    }
}

