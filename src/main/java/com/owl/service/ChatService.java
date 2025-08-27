package com.owl.service;

import com.owl.service.CacheService;
import com.owl.service.DocumentRetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ChatService {

    private final ChatClient chat;
    private final DocumentRetrievalService retrieval;
    private final CacheService cacheService;
    private final double scoreThreshold; // similarity gate

    public static class ChatRequest {
        public String tenantId;
        public String question;
        public Boolean allowWeb;
        public String document; // optional filename scoping
    }

    public static class ChatResponse {
        public String answer;
        public ChatResponse(String a) { this.answer = a; }
    }

    public ChatService(
            ChatClient chat,
            DocumentRetrievalService retrieval,
            CacheService cacheService,
            @Value("${owl.retrieval.score-threshold:0.35}") double scoreThreshold
    ) {
        this.chat = chat;
        this.retrieval = retrieval;
        this.cacheService = cacheService;
        this.scoreThreshold = scoreThreshold;
    }

    public ChatResponse answer(ChatRequest req) {
        String tenantId = req.tenantId;
        String query = (req.question == null || req.question.isBlank()) ? "overview" : req.question.trim();
        boolean allowWeb = req.allowWeb != null && req.allowWeb;

        // 0) Semantic cache (tenant-scoped)
        var cached = cacheService.lookup(tenantId, query);
        if (cached.isPresent()) {
            return new ChatResponse(cached.get());
        }

        // 1) Scored retrieval (filename-aware)
        List<DocumentRetrievalService.ScoredDocument> scored;
        if (req.document != null && !req.document.isBlank()) {
            scored = retrieval.retrieveWithScores(tenantId, req.document, query, 8, scoreThreshold);
        } else {
            scored = retrieval.retrieveWithScores(tenantId, null, query, 8, scoreThreshold);
        }

        if (scored == null || scored.isEmpty()) {
            if (allowWeb) {
                return new ChatResponse("No strong KB match. Web lookup fallback is enabled, but not implemented in MVP.");
            }
            return new ChatResponse(
                    "I couldn’t find this in your knowledge base. Try asking about specific topics " +
                            "or provide the document name (e.g., \"document\":\"income-tax-bill-2025.pdf\")."
            );
        }

        // 2) Score-based gating
        double topScore = scored.get(0).score;
        if (topScore < scoreThreshold) {
            if (allowWeb) {
                return new ChatResponse("No strong KB match. Web lookup fallback is enabled, but not implemented in MVP.");
            }
            return new ChatResponse(
                    "I don’t have enough information in your knowledge base to answer confidently. " +
                            "Try adding more docs or ask a narrower question."
            );
        }

        // 3) Build grounded prompt (compact context)
        StringBuilder kb = new StringBuilder();
        int maxCtx = Math.min(6, scored.size());
        for (int i = 0; i < maxCtx; i++) {
            Document d = scored.get(i).doc;
            String t = d.getText();
            if (t != null && !t.isBlank()) {
                kb.append(t, 0, Math.min(1200, t.length())).append("\n---\n");
            }
        }

        String sys = """
            You are Owl, a tenant-scoped assistant. Only use the provided <CONTEXT> to answer.
            If the answer is not in <CONTEXT>, say you don't know. Be concise and factual.
            Cite briefly using document names when appropriate.
            """;

        String user = "QUESTION:\n" + query + "\n\n<CONTEXT>\n" + kb + "\n</CONTEXT>";

        // 4) LLM call
        String answer = chat.prompt()
                .system(sys)
                .user(user)
                .call()
                .content();

        // 5) Append short citations (up to 3)
        StringBuilder cites = new StringBuilder();
        for (int i = 0; i < Math.min(3, scored.size()); i++) {
            Map<String,Object> md = scored.get(i).doc.getMetadata();
            if (md == null) continue;
            String title = Objects.toString(
                    md.getOrDefault("filename",
                            md.getOrDefault("title",
                                    md.getOrDefault("source", "document"))), "document");
            String url = Objects.toString(md.getOrDefault("url",""), "");
            cites.append("- ").append(title);
            if (!url.isBlank()) cites.append(" (").append(url).append(")");
            cites.append("\n");
        }
        String finalAnswer = answer + (cites.length() > 0 ? "\n\nSources:\n" + cites : "");

        // 6) Save to semantic cache
        cacheService.save(tenantId, query, finalAnswer);

        return new ChatResponse(finalAnswer);
    }
}
