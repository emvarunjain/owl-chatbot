package com.owl.retrieval.config;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class QdrantInit {
    private final EmbeddingModel embeddingModel;
    private final WebClient qdrant;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${QDRANT_VECTOR_SIZE:0}")
    private int vectorSizeOverride;
    @Value("${spring.ai.vectorstore.qdrant.initialize-schema:false}")
    private boolean initializeSchema;

    public QdrantInit(EmbeddingModel embeddingModel,
                      @Value("${QDRANT_URL:http://localhost:6333}") String qdrantUrl) {
        this.embeddingModel = embeddingModel;
        this.qdrant = WebClient.builder().baseUrl(qdrantUrl).build();
    }

    @PostConstruct
    public void ensureCollection() {
        if (!initializeSchema) return;
        int dim = (vectorSizeOverride > 0) ? vectorSizeOverride : embeddingModel.dimensions();
        boolean exists = Boolean.TRUE.equals(
                qdrant.get().uri("/collections/{name}", collectionName)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                            if (resp.statusCode() == HttpStatus.NOT_FOUND) return Mono.empty();
                            return resp.createException();
                        })
                        .bodyToMono(Map.class)
                        .map(body -> true)
                        .defaultIfEmpty(false)
                        .block());
        if (exists) return;
        Map<String, Object> payload = Map.of("vectors", Map.of("size", dim, "distance", "Cosine"));
        qdrant.put().uri("/collections/{name}", collectionName)
                .bodyValue(payload)
                .retrieve().toBodilessEntity().block();
        System.out.println("Created Qdrant collection '" + collectionName + "' with dim=" + dim);
    }
}
