package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "queues")
public class Queue {
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    private String name;
    private String description;
    private boolean isActive;
    private Set<String> agentIds; // Agents assigned to this queue
    private Set<String> campaignIds; // Campaigns using this queue
    
    // Queue settings
    private int maxWaitTime; // Max wait time in seconds
    private int maxQueueSize; // Max number of waiting chats
    private boolean allowTransfer; // Allow chat transfers
    private String transferMessage; // Message when transferring
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    public Queue() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.maxWaitTime = 300; // 5 minutes default
        this.maxQueueSize = 100;
        this.allowTransfer = true;
    }
    
    public Queue(String name, String description, String tenantId, String createdBy) {
        this();
        this.name = name;
        this.description = description;
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
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Set<String> getAgentIds() { return agentIds; }
    public void setAgentIds(Set<String> agentIds) { this.agentIds = agentIds; }
    
    public Set<String> getCampaignIds() { return campaignIds; }
    public void setCampaignIds(Set<String> campaignIds) { this.campaignIds = campaignIds; }
    
    public int getMaxWaitTime() { return maxWaitTime; }
    public void setMaxWaitTime(int maxWaitTime) { this.maxWaitTime = maxWaitTime; }
    
    public int getMaxQueueSize() { return maxQueueSize; }
    public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
    
    public boolean isAllowTransfer() { return allowTransfer; }
    public void setAllowTransfer(boolean allowTransfer) { this.allowTransfer = allowTransfer; }
    
    public String getTransferMessage() { return transferMessage; }
    public void setTransferMessage(String transferMessage) { this.transferMessage = transferMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
