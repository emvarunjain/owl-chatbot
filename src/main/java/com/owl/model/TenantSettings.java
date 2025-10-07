package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tenant_settings")
public class TenantSettings {
    @Id
    private String id;
    @Indexed(unique = true)
    private String tenantId;
    private boolean fallbackEnabled;
    private boolean guardrailsEnabled;
    private String region; // e.g., us-east-1, eu-west-1
    private String plan;   // free|pro|enterprise

    public TenantSettings() {}
    public TenantSettings(String tenantId) { this.tenantId = tenantId; }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public boolean isGuardrailsEnabled() { return guardrailsEnabled; }
    public String getRegion() { return region; }
    public String getPlan() { return plan; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    public void setGuardrailsEnabled(boolean guardrailsEnabled) { this.guardrailsEnabled = guardrailsEnabled; }
    public void setRegion(String region) { this.region = region; }
    public void setPlan(String plan) { this.plan = plan; }
}
