package com.owl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngestionServiceTest {

    @Test
    void ingestHtml_normalizes_and_adds() throws Exception {
        TenantVectorService store = mock(TenantVectorService.class);
        EventPublisher events = mock(EventPublisher.class);
        DedupService dedup = mock(DedupService.class);
        SitemapCrawler crawler = mock(SitemapCrawler.class);

        when(crawler.fetchHtml("https://ex"))
                .thenReturn("<html><main>Hello world</main></html>");
        when(crawler.normalizeHtml(anyString(), eq("https://ex")))
                .thenReturn("Hello world");
        when(dedup.recordIfNew(anyString(), anyString(), anyString())).thenReturn(true);

        IngestionService svc = new IngestionService(store, events, dedup, crawler, new DlpService(false), new RemoteRetrievalClient(""));
        int n = svc.ingestHtml("acme", "https://ex");
        assertEquals(1, n);

        ArgumentCaptor<List<Document>> docs = ArgumentCaptor.forClass((Class) List.class);
        verify(store).add(eq("acme"), docs.capture());
        List<Document> added = docs.getValue();
        assertEquals(1, added.size());
        assertEquals("acme", added.get(0).getMetadata().get("tenantId"));
        assertEquals("https://ex", added.get(0).getMetadata().get("url"));
        assertEquals("kb", added.get(0).getMetadata().get("type"));
    }

    @Test
    void ingestSitemap_iterates_urls() throws Exception {
        TenantVectorService store = mock(TenantVectorService.class);
        EventPublisher events = mock(EventPublisher.class);
        DedupService dedup = mock(DedupService.class);
        SitemapCrawler crawler = mock(SitemapCrawler.class);

        when(crawler.fetchUrls("https://site/sitemap.xml", 5))
                .thenReturn(List.of("https://a", "https://b"));
        when(crawler.fetchHtml(anyString())).thenReturn("<html><main>x</main></html>");
        when(crawler.normalizeHtml(anyString(), anyString())).thenReturn("x");
        when(dedup.recordIfNew(anyString(), anyString(), anyString())).thenReturn(true);

        IngestionService svc = new IngestionService(store, events, dedup, crawler, new DlpService(false), new RemoteRetrievalClient(""));
        int n = svc.ingestSitemap("acme", "https://site/sitemap.xml", 5);
        assertEquals(2, n);
        verify(store, atLeastOnce()).add(eq("acme"), anyList());
    }
}
