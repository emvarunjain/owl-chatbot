package com.owl.rerank;

import com.owl.service.DocumentRetrievalService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Lightweight CPU-only re-ranker using token overlap and simple heuristics.
 * Not as strong as cross-encoder BGE, but improves ordering and reduces robotic answers.
 */
@Component
public class TokenOverlapReranker implements Reranker {

    @Override
    public List<DocumentRetrievalService.Scored> rerank(String tenantId, String query, List<DocumentRetrievalService.Scored> input) {
        if (input == null || input.size() <= 1) return input;
        Set<String> q = tokenize(query);
        List<ScoredWrap> wraps = new ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) {
            var s = input.get(i);
            String text = Optional.ofNullable(s.doc().getText()).orElse("");
            Set<String> t = tokenize(text);
            int overlap = 0;
            for (String tok : q) if (t.contains(tok)) overlap++;
            double jaccard = q.isEmpty() || t.isEmpty() ? 0.0 : (double) overlap / (double) (q.size() + t.size() - overlap);
            // Combine original vector score with overlap signal.
            double combined = 0.7 * s.score() + 0.3 * jaccard;
            wraps.add(new ScoredWrap(s, combined));
        }
        wraps.sort((a, b) -> Double.compare(b.combined, a.combined));
        return wraps.stream().map(w -> w.base).toList();
    }

    private record ScoredWrap(DocumentRetrievalService.Scored base, double combined) {}

    private static Set<String> tokenize(String s) {
        String[] parts = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\s]", " ").split("\\s+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p.length() < 2) continue; // skip tiny tokens
            out.add(p);
        }
        return out;
    }
}

