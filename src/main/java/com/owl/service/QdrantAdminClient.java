package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Minimal Qdrant admin client for delete-by-filter. */
@Component
public class QdrantAdminClient {

    private final WebClient http;
    private final String collection;

    public QdrantAdminClient(@Value("${QDRANT_URL:http://localhost:6333}") String qdrantUrl,
                             @Value("${spring.ai.vectorstore.qdrant.collection-name}") String collection) {
        this.http = WebClient.builder().baseUrl(qdrantUrl).build();
        this.collection = collection;
    }

    public void purgeBySource(String tenantId, String source, boolean includeCache) {
        var must = new java.util.ArrayList<Map<String, Object>>();
        must.add(Map.of("key", "tenantId", "match", Map.of("value", tenantId)));
        must.add(Map.of("should", java.util.List.of(
                Map.of("key", "filename", "match", Map.of("value", source)),
                Map.of("key", "url", "match", Map.of("value", source))
        )));
        if (!includeCache) must.add(Map.of("key", "type", "match", Map.of("value", "kb")));
        deleteByFilter(Map.of("must", must));
    }

    public void purgeCache(String tenantId) {
        var must = java.util.List.of(
                Map.of("key", "tenantId", "match", Map.of("value", tenantId)),
                Map.of("key", "type", "match", Map.of("value", "cache"))
        );
        deleteByFilter(Map.of("must", must));
    }

    private void deleteByFilter(Map<String, Object> filter) {
        http.post()
                .uri("/collections/{c}/points/delete", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("filter", filter))
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.empty())
                .block();
    }

    private static String esc(String s) { return s.replace("'", "\\'"); }
}
