package com.owl.config;

import com.owl.security.RateLimitFilter;
import com.owl.security.ApiKeyAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Value("${owl.security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, RateLimitFilter rateLimitFilter, ApiKeyAuthFilter apiKeyFilter) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/", "/index.html", "/static/**", "/admin/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority("SCOPE_admin:read","SCOPE_admin:write","SCOPE_admin")
                .requestMatchers("/api/v2/admin/**").hasAnyAuthority("SCOPE_admin:read","SCOPE_admin:write","SCOPE_admin")
                .requestMatchers("/api/v1/ingest/**").hasAnyAuthority("SCOPE_ingest:write","SCOPE_admin:write","SCOPE_admin")
                .requestMatchers("/api/v2/connectors/**").hasAnyAuthority("SCOPE_ingest:write","SCOPE_admin:write","SCOPE_admin")
                .requestMatchers("/api/v1/chat").permitAll()
                .requestMatchers("/api/**").access((authentication, context) ->
                        securityEnabled ? AuthorityAuthorizationManager.authenticated().check(authentication, context)
                                : new AuthorizationDecision(true)) // permit if disabled
                .anyRequest().permitAll()
        );

        if (securityEnabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            http.httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }
}
