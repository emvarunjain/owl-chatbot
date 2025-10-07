package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "model_routing")
public class ModelRouting {
    @Id
    private String id;
    @Indexed(unique = true)
    private String tenantId;
    private String provider; // ollama|openai|azure|bedrock
    private String chatModel;
    private String embedModel;

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getProvider() { return provider; }
    public String getChatModel() { return chatModel; }
    public String getEmbedModel() { return embedModel; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }
}

