package com.owl.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CacheServiceTest {

    @Test
    void lookup_returns_when_above_threshold() {
        VectorStore store = mock(VectorStore.class);
        var doc = new Document("cached answer", Map.of("score", 0.95));
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        CacheService svc = new CacheService(store, 0.9);

        Optional<String> r = svc.lookup("acme", "q");
        assertTrue(r.isPresent());
        assertEquals("cached answer", r.get());
    }

    @Test
    void lookup_empty_when_below_threshold() {
        VectorStore store = mock(VectorStore.class);
        var doc = new Document("cached answer", Map.of("score", 0.5));
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        CacheService svc = new CacheService(store, 0.9);

        Optional<String> r = svc.lookup("acme", "q");
        assertTrue(r.isEmpty());
    }

    @Test
    void save_adds_document() {
        VectorStore store = mock(VectorStore.class);
        CacheService svc = new CacheService(store, 0.9);
        svc.save("acme", "q", "ans");
        verify(store).add(anyList());
    }
}

