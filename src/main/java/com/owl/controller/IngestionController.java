package com.owl.controller;

import com.owl.service.IngestionService;
import com.owl.security.TenantAuth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/ingest", "/api/v1/ingest"})
public class IngestionController {

    private final IngestionService ingestion;
    private final TenantAuth tenantAuth;

    public IngestionController(IngestionService ingestion, TenantAuth tenantAuth) {
        this.ingestion = ingestion;
        this.tenantAuth = tenantAuth;
    }

    @PostMapping("/file")
    public ResponseEntity<String> ingestFile(
            @RequestParam String tenantId,
            @RequestPart("file") MultipartFile file) throws Exception {
        tenantAuth.authorize(tenantId);
        int chunks = ingestion.ingestFile(tenantId, file);
        return ResponseEntity.ok("Ingested chunks: " + chunks);
    }

    @PostMapping("/url")
    public ResponseEntity<String> ingestUrl(
            @RequestParam String tenantId,
            @RequestParam String url) {
        tenantAuth.authorize(tenantId);
        int chunks = ingestion.ingestUrl(tenantId, url);
        return ResponseEntity.ok("Ingested chunks: " + chunks);
    }

    @PostMapping("/html")
    public ResponseEntity<String> ingestHtml(
            @RequestParam String tenantId,
            @RequestParam String url) throws Exception {
        tenantAuth.authorize(tenantId);
        int chunks = ingestion.ingestHtml(tenantId, url);
        return ResponseEntity.ok("Ingested chunks: " + chunks);
    }

    @PostMapping("/sitemap")
    public ResponseEntity<String> ingestSitemap(
            @RequestParam String tenantId,
            @RequestParam String url,
            @RequestParam(defaultValue = "50") int max) {
        tenantAuth.authorize(tenantId);
        int chunks = ingestion.ingestSitemap(tenantId, url, max);
        return ResponseEntity.ok("Ingested chunks: " + chunks);
    }
}
