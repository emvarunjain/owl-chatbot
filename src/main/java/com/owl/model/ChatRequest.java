package com.owl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatRequest(@NotBlank String tenantId,
                          @NotBlank String question,
                          boolean allowWeb,
                          String document,
                          FallbackPolicy fallback) {
    public record FallbackPolicy(Boolean enabled, Double budgetUsd, Integer maxWebCalls) {}
}
