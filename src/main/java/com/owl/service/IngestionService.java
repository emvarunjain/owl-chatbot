package com.owl.service;

import com.owl.util.PdfUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;

@Service
public class IngestionService {

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingestFile(String tenantId, MultipartFile file) throws Exception {
        String filename = (file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename()).toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType();

        Map<String, Object> meta = new HashMap<>();
        meta.put("tenantId", tenantId);
        meta.put("source", "upload");
        meta.put("filename", filename);
        meta.put("type", "kb"); // ensure every doc has type=kb

        List<Document> docs;
        if (filename.endsWith(".pdf") || contentType.toLowerCase().contains("pdf")) {
            docs = PdfUtil.readPdfAsDocuments(
                    new InputStreamResource(file.getInputStream()),
                    ExtractedTextFormatter.defaults(),
                    meta
            );
        } else {
            var reader = new TikaDocumentReader(
                    new InputStreamResource(file.getInputStream()),
                    ExtractedTextFormatter.defaults()
            );
            docs = reader.get();
            docs.forEach(d -> d.getMetadata().putAll(meta));
        }
        return ingestDocuments(meta, docs);
    }

    public int ingestUrl(String tenantId, String url) {
        var reader = new JsoupDocumentReader(url);
        List<Document> docs = reader.get();

        String filename = deriveFilenameFromUrl(url);

        Map<String, Object> meta = new HashMap<>();
        meta.put("tenantId", tenantId);
        meta.put("source", "url");
        meta.put("url", url);
        meta.put("filename", filename);
        meta.put("type", "kb");

        docs.forEach(d -> d.getMetadata().putAll(meta));

        return ingestDocuments(meta, docs);
    }

    private int ingestDocuments(Map<String, Object> sourceMeta, List<Document> docs) {
        List<Document> working = new ArrayList<>(docs);
        working.removeIf(d -> d.getText() == null || d.getText().isBlank());

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(200)
                .build();

        List<Document> chunks = splitter.apply(working);

        if (chunks.isEmpty()) {
            StringBuilder full = new StringBuilder();
            for (Document d : working) {
                if (d.getText() != null) full.append(d.getText()).append("\n");
            }
            if (full.length() == 0) {
                throw new IllegalArgumentException("No ingestible text content found in the document(s).");
            }
            Document single = new Document(full.toString(), new HashMap<>(sourceMeta));
            chunks = List.of(single);
        } else {
            for (Document c : chunks) {
                c.getMetadata().putAll(sourceMeta);
                c.getMetadata().putIfAbsent("type", "kb");
            }
        }

        vectorStore.add(chunks);
        return chunks.size();
    }

    private static String deriveFilenameFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank() || path.equals("/")) {
                return (uri.getHost() != null ? uri.getHost() : "page") + ".html";
            }
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            if (last == null || last.isBlank()) {
                return (uri.getHost() != null ? uri.getHost() : "page") + ".html";
            }
            return last.toLowerCase();
        } catch (Exception e) {
            return "page.html";
        }
    }
}
