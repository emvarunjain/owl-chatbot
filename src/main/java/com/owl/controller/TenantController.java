package com.owl.controller;

import com.owl.model.TenantCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TenantCreateRequest req) {
        // MVP: persist metadata later; for now, acknowledge.
        return ResponseEntity.ok(Map.of("status","ok","tenantId",req.tenantId(),"name",req.name()));
    }
}
