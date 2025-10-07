package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class TenantVectorService {
    private final org.springframework.ai.vectorstore.VectorStore defaultStore;
    private final boolean perTenant;
    private final QdrantTenantCollections tenantCollections;
    private final EmbeddingModel embeddings;
    private final WebClient defaultQdrant;
    private final com.owl.config.RegionConfig regions;
    private final java.util.concurrent.ConcurrentHashMap<String, WebClient> qdrantByRegion = new java.util.concurrent.ConcurrentHashMap<>();

    public TenantVectorService(org.springframework.ai.vectorstore.VectorStore defaultStore,
                               QdrantTenantCollections tenantCollections,
                               EmbeddingModel embeddings,
                               @Value("${owl.isolation.collectionPerTenant:false}") boolean perTenant,
                               @Value("${QDRANT_URL:http://localhost:6333}") String qdrantUrl,
                               com.owl.config.RegionConfig regions) {
        this.defaultStore = defaultStore;
        this.tenantCollections = tenantCollections;
        this.embeddings = embeddings;
        this.perTenant = perTenant;
        this.defaultQdrant = WebClient.builder().baseUrl(qdrantUrl).build();
        this.regions = regions;
    }

    public void add(String tenantId, List<Document> docs) {
        if (!perTenant) {
            defaultStore.add(docs);
            return;
        }
        tenantCollections.ensureTenantCollection(tenantId);
        String collection = tenantCollections.collectionName(tenantId);
        List<Map<String, Object>> points = new ArrayList<>();
        for (Document d : docs) {
            List<Double> vec = embeddings.embed(d.getText()).getResult();
            Map<String, Object> payload = new HashMap<>(d.getMetadata());
            payload.put("tenantId", tenantId);
            payload.put("text", d.getText());
            Map<String, Object> point = new HashMap<>();
            point.put("vector", vec);
            point.put("payload", payload);
            points.add(point);
        }
        Map<String, Object> body = Map.of("points", points);
        WebClient q = resolveQdrant();
        q.put()
                .uri("/collections/{c}/points?wait=true", collection)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public List<Document> search(String tenantId, String query, String scopeDocument, int topK) {
        if (!perTenant) {
            // Delegate to default VectorStore via DocumentRetrievalService; this method is not used in this mode.
            throw new UnsupportedOperationException("Direct search not supported in single-collection mode");
        }
        String collection = tenantCollections.collectionName(tenantId);
        List<Double> vec = embeddings.embed(query).getResult();
        Map<String, Object> filter = null;
        if (scopeDocument != null && !scopeDocument.isBlank()) {
            filter = Map.of("must", List.of(
                    Map.of("should", List.of(
                            Map.of("key", "filename", "match", Map.of("value", scopeDocument)),
                            Map.of("key", "url", "match", Map.of("value", scopeDocument))
                    ))
            ));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("vector", vec);
        body.put("limit", Math.max(1, topK));
        if (filter != null) body.put("filter", filter);
        WebClient q = resolveQdrant();
        Map<?, ?> resp = q.post()
                .uri("/collections/{c}/points/search", collection)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        List<Map<String, Object>> result = (List<Map<String, Object>>) resp.getOrDefault("result", List.of());
        List<Document> out = new ArrayList<>();
        for (Map<String, Object> r : result) {
            Map<String, Object> payload = (Map<String, Object>) r.get("payload");
            Object score = r.get("score");
            String text = Objects.toString(payload.getOrDefault("text", ""));
            Map<String, Object> md = new HashMap<>(payload);
            md.remove("text");
            if (score instanceof Number n) md.put("score", n.doubleValue());
            out.add(new Document(text, md));
        }
        return out;
    }

    private WebClient resolveQdrant() {
        String region = TenantRegionContext.getOverrideRegion();
        if (region == null || region.isBlank()) return defaultQdrant;
        return qdrantByRegion.computeIfAbsent(region, r -> {
            String url = regions.qdrantUrl(r, null);
            return url == null || url.isBlank() ? defaultQdrant : WebClient.builder().baseUrl(url).build();
        });
    }
}
