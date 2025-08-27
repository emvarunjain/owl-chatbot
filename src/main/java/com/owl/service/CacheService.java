package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Semantic response cache using the same VectorStore (Qdrant) as KB.
 * Cache documents carry metadata:
 *   - type=cache
 *   - tenantId=<tenant>
 *   - cachedAnswer=<final answer text>
 *   - qHash=<sha256 of normalized question + tenantId>
 *   - normalizedQuestion
 *   - createdAt=<ISO-8601>
 */
@Service
public class CacheService {

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final boolean enableCrossTenantFallback;
    private final int maxAnswerChars;

    public CacheService(
            VectorStore vectorStore,
            @Value("${owl.cache.similarity-threshold:0.90}") double similarityThreshold,
            @Value("${owl.cache.enable-cross-tenant:false}") boolean enableCrossTenantFallback,
            @Value("${owl.cache.max-answer-chars:4000}") int maxAnswerChars
    ) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
        this.enableCrossTenantFallback = enableCrossTenantFallback;
        this.maxAnswerChars = maxAnswerChars;
    }

    /** Try to find a cached answer for this question under this tenant. */
    public Optional<String> lookup(String tenantId, String question) {
        String normQ = normalize(question);

        // 1) Tenant-scoped lookup
        Optional<String> tenantHit = lookupInternal(
                "type == 'cache' && tenantId == '" + escape(tenantId) + "'",
                normQ
        );
        if (tenantHit.isPresent()) return tenantHit;

        // 2) Optional cross-tenant (common) cache
        if (enableCrossTenantFallback) {
            return lookupInternal("type == 'cache'", normQ);
        }
        return Optional.empty();
    }

    /** Save an answer into cache for this tenant+question. */
    public void save(String tenantId, String question, String answer) {
        if (answer == null || answer.isBlank()) return;

        String normQ = normalize(question);
        String qHash = sha256(tenantId + "::" + normQ);
        String trimmedAnswer = answer.length() > maxAnswerChars
                ? answer.substring(0, maxAnswerChars)
                : answer;

        Map<String, Object> meta = new HashMap<>();
        meta.put("type", "cache");
        meta.put("tenantId", tenantId);
        meta.put("normalizedQuestion", normQ);
        meta.put("qHash", qHash);
        meta.put("createdAt", Instant.now().toString());
        meta.put("cachedAnswer", trimmedAnswer);

        // Use the QUESTION as the vectorized "content" for similarity.
        Document d = new Document(question, meta);
        vectorStore.add(List.of(d));
    }

    // ---------------------- internals ----------------------

    private Optional<String> lookupInternal(String filterExpression, String normalizedQuestion) {
        SearchRequest req = SearchRequest.builder()
                .query(normalizedQuestion)
                .topK(1)
                .similarityThreshold(similarityThreshold)
                .filterExpression(filterExpression)   // <<— String filter expression
                .build();

        List<Document> hits = vectorStore.similaritySearch(req);
        if (hits == null || hits.isEmpty()) return Optional.empty();

        Document top = hits.get(0);
        Object cached = top.getMetadata().get("cachedAnswer");
        if (cached == null) return Optional.empty();

        String ans = cached.toString();
        if (ans.isBlank()) return Optional.empty();

        return Optional.of(ans);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}&&[^/.:]]", " ") // keep URL-ish chars
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static String escape(String v) {
        // basic single-quote escape for filter expressions
        return v.replace("'", "\\'");
    }
}