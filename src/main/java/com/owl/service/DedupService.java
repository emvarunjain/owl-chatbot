package com.owl.service;

import com.owl.model.DedupRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class DedupService {

    private final TenantMongoManager tenants;

    public DedupService(TenantMongoManager tenants) { this.tenants = tenants; }

    /** Returns true if this chunk text was newly recorded; false if seen before. */
    public boolean recordIfNew(String tenantId, String normalizedText, String source) {
        String hash = sha256(normalizedText);
        MongoTemplate tpl = tenants.templateForTenant(tenantId);
        ensureIndexes(tpl);
        var q = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("tenantId").is(tenantId)
                        .and("hash").is(hash)
        );
        boolean exists = tpl.exists(q, DedupRecord.class);
        if (exists) return false;
        DedupRecord rec = new DedupRecord(tenantId, hash, source);
        tpl.save(rec, "ingest_dedup");
        return true;
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureIndexes(MongoTemplate tpl) {
        IndexOperations ops = tpl.indexOps(DedupRecord.class);
        try { ops.ensureIndex(new Index().on("tenantId", Sort.Direction.ASC).named("idx_tenant")); } catch (Exception ignored) {}
        try { ops.ensureIndex(new Index().on("hash", Sort.Direction.ASC).named("idx_hash")); } catch (Exception ignored) {}
    }
}
