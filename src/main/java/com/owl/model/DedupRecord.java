package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Document(collection = "ingest_dedup")
public class DedupRecord {
    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String hash;

    private String source; // filename or url
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public DedupRecord() {}
    public DedupRecord(String tenantId, String hash, String source) {
        this.tenantId = tenantId;
        this.hash = hash;
        this.source = source;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getHash() { return hash; }
    public String getSource() { return source; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

