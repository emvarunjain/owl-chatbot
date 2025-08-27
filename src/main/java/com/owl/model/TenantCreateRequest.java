package com.owl.model;

import jakarta.validation.constraints.NotBlank;

public record TenantCreateRequest(@NotBlank String tenantId,
                                  @NotBlank String name) {}
