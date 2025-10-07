package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;

@Document(collection = "api_tokens")
public class ApiToken {
    @Id
    private String id;
    @Indexed
    private String tenantId;
    private String name;
    private List<String> scopes;
    private String tokenHash; // SHA-256 base64url
    private boolean active = true;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime lastUsedAt;

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public List<String> getScopes() { return scopes; }
    public String getTokenHash() { return tokenHash; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }

    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setName(String name) { this.name = name; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setActive(boolean active) { this.active = active; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}

