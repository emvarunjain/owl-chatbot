package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "model_credentials")
public class ModelCredentials {
    @Id
    private String id;
    @Indexed
    private String tenantId;
    private String provider; // openai|azure|bedrock
    private String apiKey;
    private String endpoint; // azure endpoint or custom
    private String azureDeployment; // for Azure OpenAI
    private String region; // for bedrock

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getProvider() { return provider; }
    public String getApiKey() { return apiKey; }
    public String getEndpoint() { return endpoint; }
    public String getAzureDeployment() { return azureDeployment; }
    public String getRegion() { return region; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setAzureDeployment(String azureDeployment) { this.azureDeployment = azureDeployment; }
    public void setRegion(String region) { this.region = region; }
}

