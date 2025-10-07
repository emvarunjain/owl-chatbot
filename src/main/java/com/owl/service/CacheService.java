package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Semantic cache: saves LLM answers as vectorized docs (type=cache) per tenant.
 * On lookup, if nearest cached answer similarity >= threshold, short-circuit the LLM.
 */
@Service
public class CacheService {

    private final VectorStore store;
    private final double threshold;

    public CacheService(VectorStore store,
                        @Value("${owl.cache.similarity-threshold:0.90}") double threshold) {
        this.store = store;
        this.threshold = threshold;
    }

    public Optional<String> lookup(String tenantId, String query) {
        // Spring AI 1.0.1: use builder(), not a static "query(...)" method.
        SearchRequest req = SearchRequest.builder()
                .query(query)
                .topK(1)
                .similarityThreshold(0.0) // disable threshold here; we enforce our own
                // You can pass a string DSL directly; it will be parsed for the vector store.
                .filterExpression("tenantId == '" + escape(tenantId) + "' && type == 'cache'")
                .build();

        var hits = store.similaritySearch(req); // disambiguates the overload
        if (hits == null || hits.isEmpty()) return Optional.empty();

        Document doc = hits.get(0);
        double score = readScore(doc);
        // In Spring AI 1.0.1, text lives on Content.getText() which Document implements.
        return score >= threshold ? Optional.of(doc.getText()) : Optional.empty();
    }

    public void save(String tenantId, String query, String answer) {
        // Store the cached answer as a Document with metadata that marks it as cache.
        var cachedDoc = new Document(answer, Map.of(
                "tenantId", tenantId,
                "type", "cache",
                "question", query
        ));
        store.add(java.util.List.of(cachedDoc));
    }

    private double readScore(Document d) {
        Object s = d.getMetadata().get("score");
        return (s instanceof Number n) ? n.doubleValue() : 0.0;
    }

    private static String escape(String s) {
        return s.replace("'", "\\'");
    }
}