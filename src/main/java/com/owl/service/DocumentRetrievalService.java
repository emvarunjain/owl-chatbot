package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Per-tenant retrieval service (vector search, MVP).
 * - Enforces tenant isolation via filterExpression.
 * - Optional scoping to a specific filename or URL.
 * - Returns a lightweight Scored wrapper; score is read from metadata when present.
 *
 * Notes on "score":
 *   Spring AI's VectorStore may (or may not) populate a similarity score in metadata
 *   (e.g., "score" or "distance"). We attempt to read "score" and default to 0.0 if absent.
 *   Callers should be robust to missing scores (e.g., rely on rank or apply a conservative threshold).
 */
@Service
public class DocumentRetrievalService {

    private final VectorStore store;
    private final TenantVectorService tenantVectors;
    private final RemoteRetrievalClient remote;

    public DocumentRetrievalService(VectorStore store, TenantVectorService tenantVectors, RemoteRetrievalClient remote) {
        this.store = store;
        this.tenantVectors = tenantVectors;
        this.remote = remote;
    }

    /** Simple wrapper carrying the Spring AI Document and an optional similarity score. */
    public record Scored(Document doc, double score) { }

    /**
     * Perform similarity search within a tenant, optionally scoped to a document (filename or URL).
     *
     * @param tenantId      required tenant identifier
     * @param query         user query
     * @param scopeDocument optional filename or URL to restrict results
     * @param topK          number of results to return
     */
    public List<Scored> search(String tenantId, String query, String scopeDocument, int topK) {
        // Prefer remote retrieval if enabled
        List<org.springframework.ai.document.Document> docs;
        if (remote != null && remote.isEnabled()) {
            docs = remote.search(tenantId, query, scopeDocument, topK);
        } else {
            String filter = buildFilter(tenantId, scopeDocument);
            try {
                docs = tenantVectors.search(tenantId, query, scopeDocument, topK);
            } catch (UnsupportedOperationException e) {
                SearchRequest req = SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build();
                docs = store.similaritySearch(req);
            }
        }
        return docs.stream().map(d -> new Scored(d, readScore(d))).toList();
    }

    private static String buildFilter(String tenantId, String scopeDocument) {
        StringBuilder sb = new StringBuilder("tenantId == '").append(escape(tenantId)).append("'");
        if (scopeDocument != null && !scopeDocument.isBlank()) {
            String esc = escape(scopeDocument);
            sb.append(" && (filename == '").append(esc).append("' || url == '").append(esc).append("')");
        }
        return sb.toString();
    }

    /** Try to read a numeric similarity "score" from metadata; default to 0.0 if absent. */
    private static double readScore(Document d) {
        Object s = d.getMetadata().get("score");
        if (s instanceof Number n) return n.doubleValue();
        // Some stores only provide rank or distance; callers should handle 0.0 as "unknown".
        return 0.0;
    }

    /** Minimal escaping for single quotes in filter expressions. */
    private static String escape(String s) {
        return s.replace("'", "\\'");
    }
}
