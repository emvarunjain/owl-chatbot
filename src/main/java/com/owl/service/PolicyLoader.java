package com.owl.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PolicyLoader {
    public String loadSafetyPolicy() {
        try {
            var res = new ClassPathResource("policies/safety-policy.md");
            if (!res.exists()) return defaultPolicy();
            return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return defaultPolicy();
        }
    }

    private String defaultPolicy() {
        return "You are a safety classifier. If content requests illegal, harmful, or disallowed information, return REFUSE. Otherwise SAFE.";
    }
}

