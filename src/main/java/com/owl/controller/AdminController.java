package com.owl.controller;

import com.owl.model.User;
import com.owl.model.UserRole;
import com.owl.model.Campaign;
import com.owl.model.Queue;
import com.owl.service.UserService;
import com.owl.service.CampaignService;
import com.owl.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private CampaignService campaignService;
    
    @Autowired
    private QueueService queueService;
    
    // User Management
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user, Authentication auth) {
        // Check if current user has permission to create users
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        String tenantId = getTenantId(auth);
        List<User> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        return userService.getUserById(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User user, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User updatedUser = userService.updateUser(userId, user);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<User> updateUserStatus(@PathVariable String userId, @RequestBody Map<String, String> status, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User updatedUser = userService.updateAgentStatus(userId, 
            com.owl.model.AgentStatus.valueOf(status.get("status")));
        return ResponseEntity.ok(updatedUser);
    }
    
    @PutMapping("/users/{userId}/queues")
    public ResponseEntity<User> assignUserToQueues(@PathVariable String userId, @RequestBody Set<String> queueIds, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User updatedUser = userService.assignToQueues(userId, queueIds);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PutMapping("/users/{userId}/campaigns")
    public ResponseEntity<User> assignUserToCampaigns(@PathVariable String userId, @RequestBody Set<String> campaignIds, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User updatedUser = userService.assignToCampaigns(userId, campaignIds);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PutMapping("/users/{userId}/permissions")
    public ResponseEntity<User> updateUserPermissions(@PathVariable String userId, @RequestBody Set<String> permissions, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        User updatedUser = userService.updatePermissions(userId, permissions);
        return ResponseEntity.ok(updatedUser);
    }
    
    // Campaign Management
    @PostMapping("/campaigns")
    public ResponseEntity<Campaign> createCampaign(@RequestBody Campaign campaign, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        campaign.setTenantId(getTenantId(auth));
        campaign.setCreatedBy(getUserId(auth));
        
        Campaign createdCampaign = campaignService.createCampaign(campaign);
        return ResponseEntity.ok(createdCampaign);
    }
    
    @GetMapping("/campaigns")
    public ResponseEntity<List<Campaign>> getCampaigns(Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        String tenantId = getTenantId(auth);
        List<Campaign> campaigns = campaignService.getCampaignsByTenant(tenantId);
        return ResponseEntity.ok(campaigns);
    }
    
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<Campaign> getCampaign(@PathVariable String campaignId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        return campaignService.getCampaignById(campaignId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/campaigns/{campaignId}")
    public ResponseEntity<Campaign> updateCampaign(@PathVariable String campaignId, @RequestBody Campaign campaign, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        Campaign updatedCampaign = campaignService.updateCampaign(campaignId, campaign);
        return ResponseEntity.ok(updatedCampaign);
    }
    
    @DeleteMapping("/campaigns/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable String campaignId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/campaigns/{campaignId}/agents")
    public ResponseEntity<Campaign> assignAgentsToCampaign(@PathVariable String campaignId, @RequestBody Set<String> agentIds, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        Campaign updatedCampaign = campaignService.assignAgentsToCampaign(campaignId, agentIds);
        return ResponseEntity.ok(updatedCampaign);
    }
    
    // Queue Management
    @PostMapping("/queues")
    public ResponseEntity<Queue> createQueue(@RequestBody Queue queue, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        queue.setTenantId(getTenantId(auth));
        queue.setCreatedBy(getUserId(auth));
        
        Queue createdQueue = queueService.createQueue(queue);
        return ResponseEntity.ok(createdQueue);
    }
    
    @GetMapping("/queues")
    public ResponseEntity<List<Queue>> getQueues(Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        String tenantId = getTenantId(auth);
        List<Queue> queues = queueService.getQueuesByTenant(tenantId);
        return ResponseEntity.ok(queues);
    }
    
    @GetMapping("/queues/{queueId}")
    public ResponseEntity<Queue> getQueue(@PathVariable String queueId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        return queueService.getQueueById(queueId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/queues/{queueId}")
    public ResponseEntity<Queue> updateQueue(@PathVariable String queueId, @RequestBody Queue queue, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        Queue updatedQueue = queueService.updateQueue(queueId, queue);
        return ResponseEntity.ok(updatedQueue);
    }
    
    @DeleteMapping("/queues/{queueId}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String queueId, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        queueService.deleteQueue(queueId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/queues/{queueId}/agents")
    public ResponseEntity<Queue> assignAgentsToQueue(@PathVariable String queueId, @RequestBody Set<String> agentIds, Authentication auth) {
        if (!hasAdminPermission(auth)) {
            return ResponseEntity.forbidden().build();
        }
        
        Queue updatedQueue = queueService.assignAgentsToQueue(queueId, agentIds);
        return ResponseEntity.ok(updatedQueue);
    }
    
    // Helper methods
    private boolean hasAdminPermission(Authentication auth) {
        // Check if user has admin or superadmin role
        return auth.getAuthorities().stream()
            .anyMatch(authority -> 
                authority.getAuthority().equals("ROLE_ADMIN") || 
                authority.getAuthority().equals("ROLE_SUPERADMIN"));
    }
    
    private String getTenantId(Authentication auth) {
        // Extract tenant ID from authentication
        // This would depend on your authentication implementation
        return "default-tenant"; // Simplified for now
    }
    
    private String getUserId(Authentication auth) {
        // Extract user ID from authentication
        return auth.getName(); // Simplified for now
    }
}