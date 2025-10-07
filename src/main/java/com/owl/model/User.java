package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    private String email;
    private String firstName;
    private String lastName;
    private String password; // Should be encrypted
    private UserRole role;
    private AgentStatus status;
    private String tenantId;
    
    // Agent specific fields
    private Set<String> queueIds; // Queues this agent can handle
    private Set<String> campaignIds; // Campaigns this agent can handle
    private String supervisorId; // Supervisor for this agent
    private List<String> subordinateIds; // Agents under this user (for supervisors)
    
    // Access control
    private Set<String> permissions; // Granular permissions
    private boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Performance tracking
    private AgentPerformance performance;
    
    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.status = AgentStatus.OFFLINE;
    }
    
    public User(String username, String email, String firstName, String lastName, UserRole role, String tenantId) {
        this();
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.tenantId = tenantId;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public Set<String> getQueueIds() { return queueIds; }
    public void setQueueIds(Set<String> queueIds) { this.queueIds = queueIds; }
    
    public Set<String> getCampaignIds() { return campaignIds; }
    public void setCampaignIds(Set<String> campaignIds) { this.campaignIds = campaignIds; }
    
    public String getSupervisorId() { return supervisorId; }
    public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }
    
    public List<String> getSubordinateIds() { return subordinateIds; }
    public void setSubordinateIds(List<String> subordinateIds) { this.subordinateIds = subordinateIds; }
    
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public AgentPerformance getPerformance() { return performance; }
    public void setPerformance(AgentPerformance performance) { this.performance = performance; }
    
    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    public boolean canManageUser(User targetUser) {
        if (this.role == UserRole.SUPERADMIN) return true;
        if (!this.tenantId.equals(targetUser.getTenantId())) return false;
        return this.role.canManage(targetUser.getRole());
    }
    
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
