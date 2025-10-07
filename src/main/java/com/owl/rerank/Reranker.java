package com.owl.rerank;

import com.owl.service.DocumentRetrievalService;
import java.util.List;

/**
 * Reranks retrieved documents for higher answer quality.
 */
public interface Reranker {
    List<DocumentRetrievalService.Scored> rerank(String tenantId, String query, List<DocumentRetrievalService.Scored> input);
}

