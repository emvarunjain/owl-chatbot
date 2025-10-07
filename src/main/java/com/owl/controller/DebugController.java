package com.owl.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    private final VectorStore vectorStore;
    public DebugController(VectorStore vectorStore) { this.vectorStore = vectorStore; }

    @GetMapping("/filenames")
    public ResponseEntity<?> filenames(@RequestParam String tenantId,
                                       @RequestParam(defaultValue = "50") int sample) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("introduction overview summary")
                        .topK(Math.min(Math.max(sample, 1), 200))
                        .filterExpression("tenantId == '" + tenantId.replace("'","\\'") + "'")
                        .build()
        );
        Set<String> names = docs.stream()
                .map(d -> {
                    Object f = d.getMetadata().get("filename");
                    if (f != null) return f.toString();
                    Object u = d.getMetadata().get("url");
                    return u == null ? "unknown" : u.toString();
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantId", tenantId);
        out.put("distinctFilenames", names);
        out.put("sampleCount", docs.size());
        return ResponseEntity.ok(out);
    }
}
