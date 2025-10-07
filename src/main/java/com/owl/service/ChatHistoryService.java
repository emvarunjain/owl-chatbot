package com.owl.service;

import com.owl.model.ChatRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Persists chat interactions into per-tenant Mongo databases.
 */
@Service
public class ChatHistoryService {

    private final TenantMongoManager tenantMongoManager;
    private final TenantKeyService keys;
    private final CryptoService crypto;

    public ChatHistoryService(TenantMongoManager tenantMongoManager, TenantKeyService keys, CryptoService crypto) {
        this.tenantMongoManager = tenantMongoManager;
        this.keys = keys;
        this.crypto = crypto;
    }

    public String save(String tenantId, String question, String answer, boolean cacheHit, List<String> sources) {
        MongoTemplate tpl = tenantMongoManager.templateForTenant(tenantId);
        ensureIndexes(tpl);
        String toStore = answer;
        ChatRecord rec = new ChatRecord(tenantId, question, toStore, cacheHit, sources);
        if (keys.isEnabled()) {
            String key = keys.getOrCreateKey(tenantId);
            var enc = crypto.encrypt(answer, key);
            rec.setAnswer(enc.ciphertextB64);
            rec.setEncrypted(true);
            rec.setIv(enc.ivB64);
        }
        tpl.save(rec);
        return rec.getId();
    }

    public ChatRecord getById(String tenantId, String id) {
        MongoTemplate tpl = tenantMongoManager.templateForTenant(tenantId);
        return tpl.findById(id, ChatRecord.class);
    }

    private void ensureIndexes(MongoTemplate tpl) {
        IndexOperations ops = tpl.indexOps(ChatRecord.class);
        // best-effort; ignore errors if exists
        try { ops.ensureIndex(new Index().on("tenantId", Sort.Direction.ASC).named("idx_tenant")); } catch (Exception ignored) {}
        try { ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_createdAt")); } catch (Exception ignored) {}
    }
}
