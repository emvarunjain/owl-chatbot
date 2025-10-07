package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PreferenceService {
    private final VectorStore store;
    private final double threshold;

    public PreferenceService(VectorStore store,
                             @Value("${owl.preference.similarity-threshold:0.92}") double threshold) {
        this.store = store;
        this.threshold = threshold;
    }

    public Optional<String> lookup(String tenantId, String query) {
        SearchRequest req = SearchRequest.builder()
                .query(query)
                .topK(1)
                .similarityThreshold(0.0)
                .filterExpression("tenantId == '" + tenantId.replace("'","\\'") + "' && type == 'pref'")
                .build();
        List<Document> hits = store.similaritySearch(req);
        if (hits == null || hits.isEmpty()) return Optional.empty();
        Document d = hits.get(0);
        double score = readScore(d);
        if (score < threshold) return Optional.empty();
        return Optional.ofNullable(d.getText());
    }

    public void save(String tenantId, String question, String answer, List<String> sources, int rating) {
        var doc = new Document(answer, Map.of(
                "tenantId", tenantId,
                "type", "pref",
                "question", question,
                "rating", rating,
                "sources", sources
        ));
        store.add(java.util.List.of(doc));
    }

    private static double readScore(Document d) {
        Object s = d.getMetadata().get("score");
        return (s instanceof Number n) ? n.doubleValue() : 0.0;
    }
}

