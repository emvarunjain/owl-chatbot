package com.owl.controller;

import com.owl.service.QdrantAdminClient;
import com.owl.service.IngestionService;
import com.owl.service.DocumentRetrievalService;
import com.owl.service.ChatMetricsService;
import com.owl.service.BudgetService;
import com.owl.security.TenantAuth;
import com.owl.service.EventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.owl.service.ApiTokenService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
public class AdminController {

    private final VectorStore vectorStore;
    private final QdrantAdminClient qdrant;
    private final IngestionService ingestion;
    private final ChatMetricsService metrics;
    private final TenantAuth tenantAuth;
    private final EventPublisher events;
    private final BudgetService budgets;
    private final ApiTokenService apiTokens;

    public AdminController(VectorStore vectorStore, QdrantAdminClient qdrant,
                           IngestionService ingestion, ChatMetricsService metrics, TenantAuth tenantAuth, EventPublisher events, BudgetService budgets, ApiTokenService apiTokens) {
        this.vectorStore = vectorStore;
        this.qdrant = qdrant;
        this.ingestion = ingestion;
        this.metrics = metrics;
        this.tenantAuth = tenantAuth;
        this.events = events;
        this.budgets = budgets;
        this.apiTokens = apiTokens;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String tenantId,
                                                            @RequestParam String q,
                                                            @RequestParam(defaultValue = "10") int k) {
        tenantAuth.authorize(tenantId);
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(q)
                        .topK(Math.min(Math.max(k, 1), 100))
                        .filterExpression("tenantId == '" + tenantId.replace("'","\\'") + "' && type == 'kb'")
                        .build()
        );
        List<Map<String, Object>> out = new ArrayList<>();
        for (var d : docs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("text", Optional.ofNullable(d.getText()).orElse(""));
            row.put("metadata", d.getMetadata());
            out.add(row);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> sources(@RequestParam String tenantId,
                                                       @RequestParam(defaultValue = "100") int sample) {
        tenantAuth.authorize(tenantId);
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("introduction overview summary")
                        .topK(Math.min(Math.max(sample, 1), 500))
                        .filterExpression("tenantId == '" + tenantId.replace("'","\\'") + "'")
                        .build()
        );
        Set<String> names = new LinkedHashSet<>();
        for (var d : docs) {
            var md = d.getMetadata();
            Object f = md.get("filename"); Object u = md.get("url");
            names.add(f != null ? f.toString() : (u != null ? u.toString() : "unknown"));
        }
        return ResponseEntity.ok(Map.of("tenantId", tenantId, "sources", names, "sampleCount", docs.size()));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Object>> purge(@RequestParam String tenantId,
                                                     @RequestParam String source,
                                                     @RequestParam(defaultValue = "false") boolean includeCache) {
        tenantAuth.authorize(tenantId);
        qdrant.purgeBySource(tenantId, source, includeCache);
        events.audit(tenantId, actor(), "PURGE", Map.of("source", source, "includeCache", includeCache));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/recrawl")
    public ResponseEntity<Map<String, Object>> recrawl(@RequestParam String tenantId,
                                                       @RequestParam String url,
                                                       @RequestParam(defaultValue = "false") boolean sitemap,
                                                       @RequestParam(defaultValue = "50") int max) throws Exception {
        tenantAuth.authorize(tenantId);
        int count = sitemap ? ingestion.ingestSitemap(tenantId, url, max) : ingestion.ingestHtml(tenantId, url);
        events.audit(tenantId, actor(), "RECRAWL", Map.of("url", url, "sitemap", sitemap, "ingested", count));
        return ResponseEntity.ok(Map.of("ingested", count));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> adminMetrics(@RequestParam String tenantId) {
        tenantAuth.authorize(tenantId);
        return ResponseEntity.ok(metrics.snapshot(tenantId));
    }

    @GetMapping("/cluster-sample")
    public ResponseEntity<Map<String, Object>> clusterSample(@RequestParam String tenantId,
                                                             @RequestParam(defaultValue = "200") int sample) {
        tenantAuth.authorize(tenantId);
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("knowledge base faq policy guide")
                        .topK(Math.min(Math.max(sample, 1), 1000))
                        .filterExpression("tenantId == '" + tenantId.replace("'","\\'") + "' && type == 'kb'")
                        .build()
        );
        java.util.Map<Long, Integer> buckets = new java.util.HashMap<>();
        for (var d : docs) {
            String text = java.util.Optional.ofNullable(d.getText()).orElse("");
            long h = com.owl.util.TextSimhash.simhash64(text);
            long key = (h >>> 48); // top 16 bits bucket
            buckets.put(key, buckets.getOrDefault(key, 0) + 1);
        }
        return ResponseEntity.ok(java.util.Map.of("tenantId", tenantId, "clusters", buckets, "sample", docs.size()));
    }

    private String actor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "anonymous" : a.getName();
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> usage(@RequestParam String tenantId) {
        tenantAuth.authorize(tenantId);
        return ResponseEntity.ok(metrics.snapshot(tenantId));
    }

    @GetMapping("/cost")
    public ResponseEntity<Map<String, Object>> cost(@RequestParam String tenantId) {
        tenantAuth.authorize(tenantId);
        return ResponseEntity.ok(budgets.snapshot(tenantId));
    }

    public record BudgetRequest(String tenantId, double monthlyBudgetUsd) {}

    @PostMapping("/budget")
    public ResponseEntity<Map<String, Object>> setBudget(@RequestBody BudgetRequest req) {
        tenantAuth.authorize(req.tenantId());
        budgets.setBudget(req.tenantId(), req.monthlyBudgetUsd());
        return ResponseEntity.ok(budgets.snapshot(req.tenantId()));
    }

    public record SettingsRequest(String tenantId, Boolean fallbackEnabled, Boolean guardrailsEnabled) {}

    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> setSettings(@RequestBody SettingsRequest req, com.owl.service.TenantSettingsService settings) {
        tenantAuth.authorize(req.tenantId());
        if (req.fallbackEnabled() != null) settings.setFallbackEnabled(req.tenantId(), req.fallbackEnabled());
        if (req.guardrailsEnabled() != null) settings.setGuardrailsEnabled(req.tenantId(), req.guardrailsEnabled());
        var s = settings.getOrCreate(req.tenantId());
        return ResponseEntity.ok(java.util.Map.of(
                "tenantId", s.getTenantId(),
                "fallbackEnabled", s.isFallbackEnabled(),
                "guardrailsEnabled", s.isGuardrailsEnabled()
        ));
    }

    public record CreateTokenRequest(String tenantId, String name, java.util.List<String> scopes) {}

    @PostMapping("/tokens")
    public ResponseEntity<Map<String, Object>> createToken(@RequestBody CreateTokenRequest req) {
        tenantAuth.authorize(req.tenantId());
        var created = apiTokens.create(req.tenantId(), req.name(), req.scopes());
        return ResponseEntity.ok(Map.of("id", created.id(), "token", created.token()));
    }

    @GetMapping("/tokens")
    public ResponseEntity<java.util.List<Map<String, Object>>> listTokens(@RequestParam String tenantId) {
        tenantAuth.authorize(tenantId);
        var list = apiTokens.list(tenantId).stream().map(t -> Map.of(
                "id", t.getId(), "name", t.getName(), "scopes", t.getScopes(), "active", t.isActive(),
                "createdAt", t.getCreatedAt(), "lastUsedAt", t.getLastUsedAt()
        )).toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/tokens/{id}")
    public ResponseEntity<Map<String, Object>> revoke(@RequestParam String tenantId, @PathVariable String id) {
        tenantAuth.authorize(tenantId);
        apiTokens.revoke(tenantId, id);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }
}
