package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Qdrant-backed retrieval that returns (Document, score).
 * Calls Qdrant /search to obtain scores and reconstruct Documents from payload.
 */
@Service
public class DocumentRetrievalService {

    private final EmbeddingModel embeddingModel;
    private final RestClient qdrant;
    private final String collection;
    private final double scoreThresholdDefault;

    public static class ScoredDocument {
        public final Document doc;
        public final double score;
        public ScoredDocument(Document d, double s) { this.doc = d; this.score = s; }
    }

    public DocumentRetrievalService(
            EmbeddingModel embeddingModel,
            @Value("${QDRANT_URL:http://qdrant:6333}") String qdrantUrl,
            @Value("${QDRANT_COLLECTION:owl_kb}") String collection,
            @Value("${owl.retrieval.score-threshold:0.35}") double scoreThresholdDefault
    ) {
        this.embeddingModel = embeddingModel;
        this.qdrant = RestClient.builder().baseUrl(qdrantUrl).build();
        this.collection = collection;
        this.scoreThresholdDefault = scoreThresholdDefault;
    }

    public List<Document> retrieve(String tenantId, String query, int k) {
        return retrieveWithScores(tenantId, null, query, k, scoreThresholdDefault)
                .stream().map(sd -> sd.doc).collect(Collectors.toList());
    }

    public List<Document> retrieveByFile(String tenantId, String filename, String query, int k) {
        return retrieveWithScores(tenantId, filename, query, k, scoreThresholdDefault)
                .stream().map(sd -> sd.doc).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<ScoredDocument> retrieveWithScores(String tenantId, String filenameOrNull, String query, int k, double minScore) {
        // Embeddings (Spring AI 1.0.x friendly)
        float[] vector = embeddingModel.embed(query); // if your method returns float[], change type accordingly

        // IMPORTANT: filter on payload.metadata.*
        Map<String, Object> tenantFilter = Map.of(
                "key", "metadata.tenantId",
                "match", Map.of("value", tenantId)
        );
        Map<String, Object> typeFilter = Map.of(
                "key", "metadata.type",
                "match", Map.of("value", "kb")
        );
        List<Map<String, Object>> must = new ArrayList<>(List.of(tenantFilter, typeFilter));
        if (filenameOrNull != null && !filenameOrNull.isBlank()) {
            must.add(Map.of(
                    "key", "metadata.filename",
                    "match", Map.of("value", filenameOrNull)
            ));
        }
        Map<String, Object> filter = Map.of("must", must);

        Map<String, Object> body = new HashMap<>();
        body.put("vector", vector);
        body.put("limit", k);
        body.put("with_payload", true);
        body.put("with_vector", false);
        body.put("filter", filter);

        Map<String, Object> resp = qdrant.post()
                .uri("/collections/{c}/points/search", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (resp == null) return List.of();

        Object resultObj = resp.get("result");
        if (!(resultObj instanceof List<?>)) return List.of();
        List<?> results = (List<?>) resultObj;

        List<ScoredDocument> out = new ArrayList<>();
        for (Object r : results) {
            if (!(r instanceof Map<?,?>)) continue;
            Map<?,?> item = (Map<?,?>) r;

            Object scoreObj = item.get("score");
            double score = (scoreObj instanceof Number n) ? n.doubleValue() : 0.0;

            Object payloadObj = item.get("payload");
            Map<String, Object> payload = (payloadObj instanceof Map<?,?> m) ? toStringObjectMap(m) : new HashMap<>();

            // Spring AI QdrantVectorStore stores content under "content" and metadata under "metadata".
            String content = asString(payload.getOrDefault("content", ""));
            Map<String,Object> metadata = payload.containsKey("metadata")
                    ? toStringObjectMap((Map<?,?>) payload.get("metadata"))
                    : new HashMap<>();

            // fill in helpful fields for citations
            metadata.putIfAbsent("tenantId", tenantId);
            if (filenameOrNull != null && !filenameOrNull.isBlank()) {
                metadata.putIfAbsent("filename", filenameOrNull);
            }
            for (String key : List.of("filename","source","title","url")) {
                if (payload.containsKey(key) && !metadata.containsKey(key)) {
                    metadata.put(key, payload.get(key));
                }
            }

            Document d = new Document(content, metadata);
            if (score >= minScore) {
                out.add(new ScoredDocument(d, score));
            }
        }
        out.sort((a,b) -> Double.compare(b.score, a.score));
        return out;
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Map<String,Object> toStringObjectMap(Map<?,?> in) {
        Map<String,Object> res = new HashMap<>();
        for (Map.Entry<?,?> e : in.entrySet()) {
            res.put(String.valueOf(e.getKey()), e.getValue());
        }
        return res;
    }
}
