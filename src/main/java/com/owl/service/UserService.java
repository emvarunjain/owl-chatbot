package com.owl.service;

import com.owl.model.User;
import com.owl.model.UserRole;
import com.owl.model.AgentStatus;
import com.owl.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User createUser(User user) {
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default values
        user.setStatus(AgentStatus.OFFLINE);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User updateUser(String userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Update fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setQueueIds(updatedUser.getQueueIds());
        existingUser.setCampaignIds(updatedUser.getCampaignIds());
        existingUser.setSupervisorId(updatedUser.getSupervisorId());
        existingUser.setPermissions(updatedUser.getPermissions());
        existingUser.setActive(updatedUser.isActive());
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        // Update password if provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        
        return userRepository.save(existingUser);
    }
    
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public List<User> getUsersByTenant(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }
    
    public List<User> getUsersByTenantAndRole(String tenantId, UserRole role) {
        return userRepository.findByTenantIdAndRole(tenantId, role);
    }
    
    public List<User> getAvailableAgents(String tenantId) {
        return userRepository.findByTenantIdAndStatus(tenantId, AgentStatus.AVAILABLE);
    }
    
    public List<User> getAvailableAgentsForQueue(String tenantId, String queueId) {
        return userRepository.findAvailableAgentsForQueue(tenantId, queueId);
    }
    
    public List<User> getAvailableAgentsForCampaign(String tenantId, String campaignId) {
        return userRepository.findAvailableAgentsForCampaign(tenantId, campaignId);
    }
    
    public List<User> getSubordinates(String tenantId, String supervisorId) {
        return userRepository.findSubordinatesBySupervisor(tenantId, supervisorId);
    }
    
    public User updateAgentStatus(String userId, AgentStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User assignToQueues(String userId, Set<String> queueIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setQueueIds(queueIds);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User assignToCampaigns(String userId, Set<String> campaignIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setCampaignIds(campaignIds);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User updatePermissions(String userId, Set<String> permissions) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setPermissions(permissions);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public void updateLastLogin(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.updateLastLogin();
        userRepository.save(user);
    }
    
    public boolean canUserManageUser(String managerId, String targetUserId) {
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        
        User target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        return manager.canManageUser(target);
    }
    
    public List<User> getUsersWithPermission(String tenantId, String permission) {
        return userRepository.findUsersWithPermission(tenantId, permission);
    }
}
