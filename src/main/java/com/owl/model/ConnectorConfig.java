package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Map;

@Document(collection = "connectors")
public class ConnectorConfig {
    @Id
    private String id;
    @Indexed
    private String tenantId;
    private String type; // gdrive|confluence|notion|s3|...
    private Map<String, Object> config; // credentials/ids
    private String status; // created|running|paused|error
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getType() { return type; }
    public Map<String, Object> getConfig() { return config; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setType(String type) { this.type = type; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public void setStatus(String status) { this.status = status; }
}

