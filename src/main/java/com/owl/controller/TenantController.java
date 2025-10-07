package com.owl.controller;

import com.owl.model.Tenant;
import com.owl.service.TenantService;
import com.owl.security.TenantAuth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/tenants", "/api/v1/tenants"})
public class TenantController {

    private final TenantService service;
    private final TenantAuth tenantAuth;

    public TenantController(TenantService service, TenantAuth tenantAuth) {
        this.service = service;
        this.tenantAuth = tenantAuth;
    }

    // DTOs (kept local to avoid collision with any existing DTOs you may have)
    public record TenantCreateRequest(@NotBlank String tenantId, @NotBlank String name) {}
    public record TenantUpdateRequest(@NotBlank String name) {}

    @PostMapping
    public ResponseEntity<Tenant> create(@Valid @RequestBody TenantCreateRequest req) {
        Tenant t = service.create(req.tenantId(), req.name());
        return ResponseEntity.ok(t);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> get(@PathVariable String tenantId) {
        tenantAuth.authorize(tenantId);
        return ResponseEntity.ok(service.get(tenantId));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<Tenant> update(@PathVariable String tenantId,
                                         @Valid @RequestBody TenantUpdateRequest req) {
        tenantAuth.authorize(tenantId);
        return ResponseEntity.ok(service.update(tenantId, req.name()));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> delete(@PathVariable String tenantId) {
        tenantAuth.authorize(tenantId);
        service.delete(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<Tenant>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(page, size));
    }
}
