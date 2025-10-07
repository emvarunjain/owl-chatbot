package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "live_chats")
public class LiveChat {
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String agentId;
    private String agentName;
    private String queueId;
    private String campaignId;
    
    private ChatStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    
    // Chat metadata
    private String subject;
    private String priority;
    private List<String> tags;
    private String transferHistory; // JSON string of transfer history
    
    // Performance tracking
    private int messageCount;
    private double averageResponseTime;
    private boolean customerSatisfied;
    private String satisfactionRating;
    
    public LiveChat() {
        this.startedAt = LocalDateTime.now();
        this.status = ChatStatus.WAITING;
        this.messageCount = 0;
        this.averageResponseTime = 0.0;
    }
    
    public LiveChat(String tenantId, String customerId, String customerName, String queueId, String campaignId) {
        this();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.queueId = queueId;
        this.campaignId = campaignId;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }
    
    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
    
    public ChatStatus getStatus() { return status; }
    public void setStatus(ChatStatus status) { this.status = status; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getTransferHistory() { return transferHistory; }
    public void setTransferHistory(String transferHistory) { this.transferHistory = transferHistory; }
    
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    
    public double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    
    public boolean isCustomerSatisfied() { return customerSatisfied; }
    public void setCustomerSatisfied(boolean customerSatisfied) { this.customerSatisfied = customerSatisfied; }
    
    public String getSatisfactionRating() { return satisfactionRating; }
    public void setSatisfactionRating(String satisfactionRating) { this.satisfactionRating = satisfactionRating; }
    
    // Helper methods
    public void assignToAgent(String agentId, String agentName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.assignedAt = LocalDateTime.now();
        this.status = ChatStatus.ASSIGNED;
    }
    
    public void acceptChat() {
        this.acceptedAt = LocalDateTime.now();
        this.status = ChatStatus.ACTIVE;
    }
    
    public void endChat() {
        this.endedAt = LocalDateTime.now();
        this.status = ChatStatus.ENDED;
    }
    
    public void transferChat(String newAgentId, String newAgentName) {
        // Add to transfer history
        String transfer = String.format("Transferred from %s to %s at %s", 
            this.agentName, newAgentName, LocalDateTime.now());
        this.transferHistory = this.transferHistory == null ? transfer : 
            this.transferHistory + "; " + transfer;
        
        this.agentId = newAgentId;
        this.agentName = newAgentName;
        this.assignedAt = LocalDateTime.now();
        this.status = ChatStatus.ASSIGNED;
    }
    
    public long getDuration() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).toMinutes();
    }
    
    public enum ChatStatus {
        WAITING("Waiting", "Chat is waiting for agent assignment"),
        ASSIGNED("Assigned", "Chat has been assigned to an agent"),
        ACTIVE("Active", "Chat is active with agent"),
        ENDED("Ended", "Chat has ended"),
        TRANSFERRED("Transferred", "Chat has been transferred"),
        ABANDONED("Abandoned", "Customer abandoned the chat");
        
        private final String displayName;
        private final String description;
        
        ChatStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
