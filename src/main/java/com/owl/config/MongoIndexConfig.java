package com.owl.config;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;
import java.util.Objects;

@Configuration
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    /**
     * Ensure safe indexes for core collections in the shared DB.
     * - tenants: index on tenantId (unique optional later)
     */
    @Bean
    CommandLineRunner tenantIndexes(MongoTemplate mongo) {
        return args -> {
            ensureSingleFieldIndex(mongo, "tenants", "tenantId", "idx_tenantId", false);
        };
    }

    /**
     * Create an index { field: 1/-1 } iff there isn't already ANY index with the same key spec.
     */
    private void ensureSingleFieldIndex(MongoTemplate mongo, String collection,
                                        String field, String desiredName, boolean unique) {
        IndexOperations ops = mongo.indexOps(collection);
        if (hasIndexOnFields(ops, new String[]{field})) {
            log.info("Index on {}.{} already exists; skipping.", collection, field);
            return;
        }
        Index index = new Index().on(field, Sort.Direction.ASC).named(desiredName);
        if (unique) index.unique();
        try {
            ops.ensureIndex(index);
            log.info("Created index {} on {}.{}", desiredName, collection, field);
        } catch (Exception e) {
            log.warn("Non-fatal: could not create index {} on {}.{}: {}", desiredName, collection, field, e.getMessage());
        }
    }

    /**
     * Create a compound index { f1: dir1, f2: dir2, ... } iff there isn't already ANY index with same key order.
     */
    private void ensureCompoundIndex(MongoTemplate mongo, String collection,
                                     String[] fields, Sort.Direction[] dirs, String desiredName) {
        if (fields.length != dirs.length) throw new IllegalArgumentException("fields and dirs size mismatch");
        IndexOperations ops = mongo.indexOps(collection);
        if (hasIndexOnFields(ops, fields)) {
            log.info("Compound index on {}.{} already exists; skipping.", collection, String.join(",", fields));
            return;
        }
        Index idx = new Index();
        for (int i = 0; i < fields.length; i++) {
            idx = idx.on(fields[i], dirs[i]);
        }
        idx = idx.named(desiredName);
        try {
            ops.ensureIndex(idx);
            log.info("Created compound index {} on {}.{}", desiredName, collection, String.join(",", fields));
        } catch (Exception e) {
            log.warn("Non-fatal: could not create compound index {} on {}: {}", desiredName, collection, e.getMessage());
        }
    }

    /**
     * Returns true if any existing index has exactly the same key sequence.
     */
    private boolean hasIndexOnFields(IndexOperations ops, String[] fields) {
        List<IndexInfo> infos = ops.getIndexInfo();
        for (IndexInfo info : infos) {
            // Build a pipe-delimited sequence from index keys, e.g. "tenantId|createdAt"
            String sequence = info.getIndexFields().stream()
                    .map(f -> f.getKey())
                    .reduce((a, b) -> a + "|" + b).orElse("");
            String desired = String.join("|", fields);
            if (Objects.equals(sequence, desired)) {
                return true;
            }
        }
        return false;
    }
}
