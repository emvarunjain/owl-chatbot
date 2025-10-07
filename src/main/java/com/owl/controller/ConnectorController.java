package com.owl.controller;

import com.owl.security.TenantAuth;
import com.owl.service.ConnectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping({"/api/v2/connectors", "/api/connectors/v2"})
public class ConnectorController {
    private final ConnectorService connectors;
    private final TenantAuth auth;

    public ConnectorController(ConnectorService connectors, TenantAuth auth) { this.connectors = connectors; this.auth = auth; }

    @GetMapping("/gdrive/auth/start")
    public ResponseEntity<Map<String, Object>> gdriveStart(@RequestParam String tenantId, @RequestParam String redirectUri) {
        auth.authorize(tenantId);
        // Placeholder OAuth URL; replace with real Google OAuth consent URL for Drive scope
        String url = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&scope=" +
                URLEncoder.encode("https://www.googleapis.com/auth/drive.readonly", StandardCharsets.UTF_8) +
                "&client_id=YOUR_CLIENT_ID&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(tenantId, StandardCharsets.UTF_8);
        return ResponseEntity.ok(Map.of("authUrl", url));
    }

    @PostMapping("/gdrive/auth/callback")
    public ResponseEntity<Map<String, Object>> gdriveCallback(@RequestParam String tenantId, @RequestParam String code) {
        auth.authorize(tenantId);
        // Exchange code for token with Google OAuth (not implemented here)
        // Store tokens in connector config via connectors service (type=gdrive)
        connectors.create(tenantId, "gdrive", Map.of("accessToken", "stub", "refreshToken", "stub", "expiresAt", System.currentTimeMillis()+3600_000));
        return ResponseEntity.ok(Map.of("status","linked"));
    }
}

