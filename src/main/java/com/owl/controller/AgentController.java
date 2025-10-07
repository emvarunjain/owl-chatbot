package com.owl.controller;

import com.owl.model.User;
import com.owl.model.AgentStatus;
import com.owl.model.LiveChat;
import com.owl.model.AgentPerformance;
import com.owl.service.UserService;
import com.owl.service.LiveChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LiveChatService liveChatService;
    
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Authentication auth) {
        String userId = getUserId(auth);
        return userService.getUserById(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/status")
    public ResponseEntity<User> updateStatus(@RequestBody Map<String, String> request, Authentication auth) {
        String userId = getUserId(auth);
        AgentStatus status = AgentStatus.valueOf(request.get("status"));
        
        User updatedUser = userService.updateAgentStatus(userId, status);
        return ResponseEntity.ok(updatedUser);
    }
    
    @GetMapping("/chats")
    public ResponseEntity<List<LiveChat>> getMyChats(Authentication auth) {
        String agentId = getUserId(auth);
        List<LiveChat> chats = liveChatService.getChatsByAgent(agentId);
        return ResponseEntity.ok(chats);
    }
    
    @GetMapping("/chats/active")
    public ResponseEntity<List<LiveChat>> getActiveChats(Authentication auth) {
        String agentId = getUserId(auth);
        List<LiveChat> chats = liveChatService.getActiveChatsByAgent(agentId);
        return ResponseEntity.ok(chats);
    }
    
    @PostMapping("/chats/{chatId}/accept")
    public ResponseEntity<LiveChat> acceptChat(@PathVariable String chatId, Authentication auth) {
        String agentId = getUserId(auth);
        LiveChat chat = liveChatService.acceptChat(chatId, agentId);
        return ResponseEntity.ok(chat);
    }
    
    @PostMapping("/chats/{chatId}/transfer")
    public ResponseEntity<LiveChat> transferChat(@PathVariable String chatId, @RequestBody Map<String, String> request, Authentication auth) {
        String fromAgentId = getUserId(auth);
        String toAgentId = request.get("toAgentId");
        
        LiveChat chat = liveChatService.transferChat(chatId, fromAgentId, toAgentId);
        return ResponseEntity.ok(chat);
    }
    
    @PostMapping("/chats/{chatId}/end")
    public ResponseEntity<LiveChat> endChat(@PathVariable String chatId, Authentication auth) {
        String agentId = getUserId(auth);
        LiveChat chat = liveChatService.endChat(chatId, agentId);
        return ResponseEntity.ok(chat);
    }
    
    @GetMapping("/performance")
    public ResponseEntity<AgentPerformance> getPerformance(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication auth) {
        
        String agentId = getUserId(auth);
        
        // Default to current month if no period specified
        if (period == null) {
            period = "month";
        }
        
        LocalDateTime start, end;
        if (startDate != null && endDate != null) {
            start = LocalDateTime.parse(startDate);
            end = LocalDateTime.parse(endDate);
        } else {
            // Calculate date range based on period
            LocalDateTime now = LocalDateTime.now();
            switch (period.toLowerCase()) {
                case "day":
                    start = now.toLocalDate().atStartOfDay();
                    end = now;
                    break;
                case "week":
                    start = now.minusWeeks(1);
                    end = now;
                    break;
                case "month":
                    start = now.minusMonths(1);
                    end = now;
                    break;
                case "year":
                    start = now.minusYears(1);
                    end = now;
                    break;
                default:
                    start = now.minusMonths(1);
                    end = now;
            }
        }
        
        // Get performance data
        AgentPerformance performance = getAgentPerformance(agentId, start, end);
        return ResponseEntity.ok(performance);
    }
    
    @GetMapping("/subordinates")
    public ResponseEntity<List<User>> getSubordinates(Authentication auth) {
        String supervisorId = getUserId(auth);
        String tenantId = getTenantId(auth);
        
        List<User> subordinates = userService.getSubordinates(tenantId, supervisorId);
        return ResponseEntity.ok(subordinates);
    }
    
    @GetMapping("/subordinates/performance")
    public ResponseEntity<List<Map<String, Object>>> getSubordinatesPerformance(
            @RequestParam(required = false) String period,
            Authentication auth) {
        
        String supervisorId = getUserId(auth);
        String tenantId = getTenantId(auth);
        
        List<User> subordinates = userService.getSubordinates(tenantId, supervisorId);
        
        // Get performance for each subordinate
        List<Map<String, Object>> performanceData = subordinates.stream()
            .map(subordinate -> {
                AgentPerformance perf = getAgentPerformance(subordinate.getId(), 
                    LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                
                return Map.of(
                    "agentId", subordinate.getId(),
                    "agentName", subordinate.getFullName(),
                    "performance", perf
                );
            })
            .toList();
        
        return ResponseEntity.ok(performanceData);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<User>> getAvailableAgents(Authentication auth) {
        String tenantId = getTenantId(auth);
        List<User> availableAgents = userService.getAvailableAgents(tenantId);
        return ResponseEntity.ok(availableAgents);
    }
    
    @GetMapping("/queues")
    public ResponseEntity<List<com.owl.model.Queue>> getMyQueues(Authentication auth) {
        String agentId = getUserId(auth);
        String tenantId = getTenantId(auth);
        
        // This would require QueueService integration
        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }
    
    @GetMapping("/campaigns")
    public ResponseEntity<List<com.owl.model.Campaign>> getMyCampaigns(Authentication auth) {
        String agentId = getUserId(auth);
        String tenantId = getTenantId(auth);
        
        // This would require CampaignService integration
        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }
    
    // Helper methods
    private String getUserId(Authentication auth) {
        return auth.getName();
    }
    
    private String getTenantId(Authentication auth) {
        return "default-tenant"; // Simplified for now
    }
    
    private AgentPerformance getAgentPerformance(String agentId, LocalDateTime start, LocalDateTime end) {
        // This would integrate with actual performance calculation
        // For now, return a mock performance object
        AgentPerformance performance = new AgentPerformance();
        performance.setTotalChats(50);
        performance.setCompletedChats(45);
        performance.setTransferredChats(5);
        performance.setAverageResponseTime(30.5);
        performance.setAverageChatDuration(15.2);
        performance.setCustomerSatisfactionScore(4.2);
        performance.setTotalMessages(250);
        performance.setLastUpdated(LocalDateTime.now());
        
        return performance;
    }
}
