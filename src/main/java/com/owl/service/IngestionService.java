package com.owl.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Ingests PDFs/Office docs via Tika, and single URLs via Jsoup.
 * Each chunk gets tenantId + source metadata (filename or url).
 * (No ExtractedTextFormatter used -> compatible with Spring AI 1.0.1)
 */
@Service
public class IngestionService {

    private final TenantVectorService tenantVectors;
    private final EventPublisher events;
    private final DedupService dedup;
    private final SitemapCrawler crawler;
    private final DlpService dlp;
    private final RemoteRetrievalClient remote;

    public IngestionService(TenantVectorService tenantVectors, EventPublisher events, DedupService dedup, SitemapCrawler crawler, DlpService dlp, RemoteRetrievalClient remote) {
        this.tenantVectors = tenantVectors;
        this.events = events;
        this.dedup = dedup;
        this.crawler = crawler;
        this.dlp = dlp;
        this.remote = remote;
    }

    public int ingestFile(String tenantId, MultipartFile file) throws Exception {
        try (var in = file.getInputStream()) {
            // Simple constructor works across versions
            var reader = new TikaDocumentReader(new InputStreamResource(in));
            var docs = reader.get();
            int count = persist(tenantId, docs, Map.of(
                    "filename", file.getOriginalFilename(),
                    "type", "kb"
            ));
            events.ingested(tenantId, String.valueOf(file.getOriginalFilename()), count);
            return count;
        }
    }

    public int ingestUrl(String tenantId, String url) {
        var reader = new JsoupDocumentReader(url);
        var docs = reader.get();
        int count = persist(tenantId, docs, Map.of(
                "url", url,
                "type", "kb"
        ));
        events.ingested(tenantId, url, count);
        return count;
    }

    public int ingestHtml(String tenantId, String url) throws Exception {
        String html = crawler.fetchHtml(url);
        String text = crawler.normalizeHtml(html, url);
        var doc = new Document(text, Map.of("url", url, "type", "kb"));
        int count = persist(tenantId, java.util.List.of(doc), Map.of("url", url, "type", "kb"));
        events.ingested(tenantId, url, count);
        return count;
    }

    public int ingestSitemap(String tenantId, String sitemapUrl, int maxUrls) {
        int total = 0;
        for (String url : crawler.fetchUrls(sitemapUrl, maxUrls)) {
            try { total += ingestHtml(tenantId, url); } catch (Exception ignored) {}
        }
        return total;
    }

    public int ingestText(String tenantId, String source, String text) {
        var base = new Document(text, Map.of(
                source != null && source.startsWith("http") ? "url" : "filename", source == null ? "text" : source,
                "type", "kb"
        ));
        return persist(tenantId, java.util.List.of(base), Map.of(
                source != null && source.startsWith("http") ? "url" : "filename", source == null ? "text" : source,
                "type", "kb"
        ));
    }

    private int persist(String tenantId, List<Document> docs, Map<String, Object> baseMeta) {
        var chunks = new TokenTextSplitter().apply(docs);
        var out = new java.util.ArrayList<Document>(chunks.size());
        String source = (String) baseMeta.getOrDefault("filename", baseMeta.getOrDefault("url", "doc"));
        for (var d : chunks) {
            String normalized = d.getText().replaceAll("\\s+", " ").trim();
            normalized = dlp.redact(normalized);
            if (!dedup.recordIfNew(tenantId, normalized, source)) {
                continue; // skip duplicate chunk
            }
            d.getMetadata().putAll(baseMeta);
            d.getMetadata().put("tenantId", tenantId);
            // Replace text with redacted/normalized content we used for dedup
            var newDoc = new Document(normalized, d.getMetadata());
            out.add(newDoc);
        }
        if (!out.isEmpty()) {
            if (remote != null && remote.isEnabled()) remote.add(tenantId, out);
            else tenantVectors.add(tenantId, out);
        }
        return out.size();
    }
}
