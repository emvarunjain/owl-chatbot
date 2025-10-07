package com.owl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentRetrievalServiceTest {

    @Test
    void filter_byTenant_only() {
        VectorStore store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        TenantVectorService router = mock(TenantVectorService.class);
        doThrow(new UnsupportedOperationException()).when(router).search(anyString(), anyString(), any(), anyInt());
        DocumentRetrievalService svc = new DocumentRetrievalService(store, router);

        svc.search("acme", "q", null, 3);

        ArgumentCaptor<SearchRequest> cap = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(cap.capture());
        assertTrue(cap.getValue().getFilterExpression().contains("tenantId == 'acme'"));
    }

    @Test
    void filter_byTenant_andScope() {
        VectorStore store = mock(VectorStore.class);
        var d = new Document("t", Map.of("score", 0.9));
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d));
        TenantVectorService router = mock(TenantVectorService.class);
        doThrow(new UnsupportedOperationException()).when(router).search(anyString(), anyString(), any(), anyInt());
        DocumentRetrievalService svc = new DocumentRetrievalService(store, router);

        var res = svc.search("acme", "q", "file.pdf", 2);
        assertEquals(1, res.size());
        assertEquals(0.9, res.get(0).score(), 1e-6);

        ArgumentCaptor<SearchRequest> cap = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(cap.capture());
        String f = cap.getValue().getFilterExpression();
        assertTrue(f.contains("tenantId == 'acme'"));
        assertTrue(f.contains("filename == 'file.pdf'"));
        assertTrue(f.contains("url == 'file.pdf'"));
    }
}
