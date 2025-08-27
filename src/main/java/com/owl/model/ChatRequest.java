package com.owl.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String tenantId,
                          @NotBlank String question,
                          boolean allowWeb,
                          String document) {}