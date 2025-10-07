package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "campaigns")
public class Campaign {
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    private String name;
    private String description;
    private CampaignType type;
    private boolean isActive;
    private Set<String> queueIds; // Queues associated with this campaign
    private Set<String> agentIds; // Agents assigned to this campaign
    
    // Campaign settings
    private int maxConcurrentChats; // Max chats per agent
    private int responseTimeoutSeconds; // Timeout for agent response
    private boolean autoAssign; // Auto-assign incoming chats
    private String welcomeMessage; // Default welcome message
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    public Campaign() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.maxConcurrentChats = 1;
        this.responseTimeoutSeconds = 30;
        this.autoAssign = true;
    }
    
    public Campaign(String name, String description, CampaignType type, String tenantId, String createdBy) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.tenantId = tenantId;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public CampaignType getType() { return type; }
    public void setType(CampaignType type) { this.type = type; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Set<String> getQueueIds() { return queueIds; }
    public void setQueueIds(Set<String> queueIds) { this.queueIds = queueIds; }
    
    public Set<String> getAgentIds() { return agentIds; }
    public void setAgentIds(Set<String> agentIds) { this.agentIds = agentIds; }
    
    public int getMaxConcurrentChats() { return maxConcurrentChats; }
    public void setMaxConcurrentChats(int maxConcurrentChats) { this.maxConcurrentChats = maxConcurrentChats; }
    
    public int getResponseTimeoutSeconds() { return responseTimeoutSeconds; }
    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) { this.responseTimeoutSeconds = responseTimeoutSeconds; }
    
    public boolean isAutoAssign() { return autoAssign; }
    public void setAutoAssign(boolean autoAssign) { this.autoAssign = autoAssign; }
    
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum CampaignType {
        SALES("Sales", "Sales campaign for lead generation and conversion"),
        SUPPORT("Support", "Customer support campaign"),
        MARKETING("Marketing", "Marketing campaign for promotions"),
        GENERAL("General", "General purpose campaign");
        
        private final String displayName;
        private final String description;
        
        CampaignType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
