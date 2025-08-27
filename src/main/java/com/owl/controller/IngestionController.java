package com.owl.controller;

import com.owl.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final IngestionService ingestion;

    public IngestionController(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/file")
    public ResponseEntity<?> ingestFile(@RequestParam String tenantId,
                                        @RequestParam("file") MultipartFile file) throws Exception {
        int chunks = ingestion.ingestFile(tenantId, file);

        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tenantId);
        body.put("file", file.getOriginalFilename());
        body.put("bytes", file.getSize());
        body.put("chunks", chunks);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/url")
    public ResponseEntity<?> ingestUrl(@RequestParam String tenantId,
                                       @RequestParam String url) {
        int chunks = ingestion.ingestUrl(tenantId, url);

        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tenantId);
        body.put("url", url);
        body.put("chunks", chunks);

        return ResponseEntity.ok(body);
    }
}
