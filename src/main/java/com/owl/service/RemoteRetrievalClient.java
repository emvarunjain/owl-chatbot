package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RemoteRetrievalClient {
    private final WebClient http;
    private final boolean enabled;

    public RemoteRetrievalClient(@Value("${owl.retrieval.remote.url:}") String baseUrl) {
        this.enabled = baseUrl != null && !baseUrl.isBlank();
        this.http = (this.enabled ? WebClient.builder().baseUrl(baseUrl).build() : null);
    }

    public boolean isEnabled() { return enabled; }

    public List<Document> search(String tenantId, String q, String document, int topK) {
        if (!enabled) throw new IllegalStateException("Remote retrieval not enabled");
        Map<String, Object> req = Map.of(
                "tenantId", tenantId,
                "q", q,
                "document", document,
                "topK", topK
        );
        Map<?, ?> res = http.post().uri("/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        List<Map<String, Object>> docs = (List<Map<String, Object>>) res.getOrDefault("docs", List.of());
        List<Document> out = new ArrayList<>();
        for (var d : docs) {
            String text = (String) d.getOrDefault("text", "");
            Map<String, Object> md = (Map<String, Object>) d.getOrDefault("metadata", Map.of());
            out.add(new Document(text, md));
        }
        return out;
    }

    public int add(String tenantId, List<Document> docs) {
        if (!enabled) throw new IllegalStateException("Remote retrieval not enabled");
        List<Map<String, Object>> dd = new ArrayList<>();
        for (var d : docs) {
            dd.add(Map.of("text", d.getText(), "metadata", d.getMetadata()));
        }
        Map<String, Object> req = Map.of("tenantId", tenantId, "docs", dd);
        Map<?, ?> res = http.post().uri("/v1/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        Object n = res.get("added");
        return (n instanceof Number num) ? num.intValue() : dd.size();
    }
}

