package com.owl.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Service
public class PromptCacheService {

    private final TenantMongoManager tenants;

    public PromptCacheService(TenantMongoManager tenants) {
        this.tenants = tenants;
    }

    public String lookup(String tenantId, String model, String prompt) {
        MongoTemplate tpl = tenants.templateForTenant(tenantId);
        ensureIndex(tpl);
        String key = hash(model + "|" + prompt);
        Map doc = tpl.findOne(Query.query(Criteria.where("_id").is(key)), Map.class, "prompt_cache");
        return doc == null ? null : (String) doc.get("answer");
    }

    public void save(String tenantId, String model, String prompt, String answer) {
        MongoTemplate tpl = tenants.templateForTenant(tenantId);
        ensureIndex(tpl);
        String key = hash(model + "|" + prompt);
        tpl.save(Map.of("_id", key, "model", model, "answer", answer, "createdAt", System.currentTimeMillis()), "prompt_cache");
    }

    private void ensureIndex(MongoTemplate tpl) {
        IndexOperations ops = tpl.indexOps("prompt_cache");
        try {
            ops.ensureIndex(new Index()
                    .on("createdAt", org.springframework.data.domain.Sort.Direction.DESC)
                    .expire(java.time.Duration.ofDays(7)));
        } catch (Exception ignored) {}
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
