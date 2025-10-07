package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class QdrantTenantCollections {
    private final WebClient http;
    private final boolean perTenant;
    private final int vectorSize;

    public QdrantTenantCollections(@Value("${QDRANT_URL:http://localhost:6333}") String url,
                                   @Value("${owl.isolation.collectionPerTenant:false}") boolean perTenant,
                                   @Value("${QDRANT_VECTOR_SIZE:1536}") int vectorSize) {
        this.http = WebClient.builder().baseUrl(url).build();
        this.perTenant = perTenant;
        this.vectorSize = vectorSize > 0 ? vectorSize : 1536;
    }

    public void ensureTenantCollection(String tenantId) {
        if (!perTenant) return;
        String name = collectionName(tenantId);
        Map<String, Object> payload = Map.of(
                "vectors", Map.of("size", vectorSize, "distance", "Cosine")
        );
        http.put().uri("/collections/{c}", name).bodyValue(payload).retrieve().toBodilessEntity().block();
    }

    public String collectionName(String tenantId) {
        String region = TenantRegionContext.getOverrideRegion();
        if (region == null || region.isBlank()) region = "us-east-1";
        return ("owl_" + region + "_kb_" + tenantId).replaceAll("[^a-zA-Z0-9_]+","_");
    }
}
