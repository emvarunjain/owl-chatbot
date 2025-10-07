package com.owl.retrieval.api;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class RetrievalController {

    private final VectorStore store;
    public RetrievalController(VectorStore store) { this.store = store; }

    @PostMapping("/search")
    public ResponseEntity<Dto.SearchRes> search(@RequestBody Dto.SearchReq req) {
        String filter = buildFilter(req.tenantId(), req.document());
        SearchRequest s = SearchRequest.builder()
                .query(req.q())
                .topK(req.topK() > 0 ? req.topK() : 5)
                .similarityThreshold(0.0)
                .filterExpression(filter)
                .build();
        List<Document> docs = store.similaritySearch(s);
        List<Dto.Doc> out = new ArrayList<>();
        for (var d : docs) out.add(new Dto.Doc(d.getText(), d.getMetadata()));
        return ResponseEntity.ok(new Dto.SearchRes(out));
    }

    @PostMapping("/add")
    public ResponseEntity<Dto.AddRes> add(@RequestBody Dto.AddReq req) {
        List<Document> docs = new ArrayList<>();
        for (var in : req.docs()) {
            Map<String,Object> md = in.metadata() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(in.metadata());
            md.put("tenantId", req.tenantId());
            docs.add(new Document(in.text(), md));
        }
        store.add(docs);
        return ResponseEntity.ok(new Dto.AddRes(docs.size()));
    }

    private static String buildFilter(String tenantId, String scopeDocument) {
        StringBuilder sb = new StringBuilder("tenantId == '").append(tenantId.replace("'","\\'")).append("'");
        if (scopeDocument != null && !scopeDocument.isBlank()) {
            String esc = scopeDocument.replace("'","\\'");
            sb.append(" && (filename == '").append(esc).append("' || url == '").append(esc).append("')");
        }
        return sb.toString();
    }
}

