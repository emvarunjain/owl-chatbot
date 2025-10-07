package com.owl.service;

import com.owl.model.LiveChat;
import com.owl.model.User;
import com.owl.model.AgentStatus;
import com.owl.repo.LiveChatRepository;
import com.owl.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class LiveChatService {
    
    @Autowired
    private LiveChatRepository liveChatRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public LiveChat createChat(LiveChat chat) {
        chat.setStartedAt(LocalDateTime.now());
        chat.setStatus(LiveChat.ChatStatus.WAITING);
        
        LiveChat savedChat = liveChatRepository.save(chat);
        
        // Send to Kafka for real-time processing
        kafkaTemplate.send("chat-created", savedChat);
        
        // Auto-assign if enabled
        if (shouldAutoAssign(chat)) {
            assignChatToAvailableAgent(savedChat.getId());
        }
        
        return savedChat;
    }
    
    public LiveChat assignChatToAgent(String chatId, String agentId) {
        LiveChat chat = liveChatRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        
        User agent = userRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        
        // Check if agent is available
        if (agent.getStatus() != AgentStatus.AVAILABLE) {
            throw new IllegalStateException("Agent is not available");
        }
        
        // Check if agent can handle more chats
        List<LiveChat> activeChats = liveChatRepository.findActiveChatsByAgent(agentId);
        if (activeChats.size() >= getMaxConcurrentChats(agent)) {
            throw new IllegalStateException("Agent has reached maximum concurrent chats");
        }
        
        // Assign chat
        chat.assignToAgent(agentId, agent.getFullName());
        chat.setAssignedAt(LocalDateTime.now());
        
        // Update agent status
        agent.setStatus(AgentStatus.BUSY);
        userRepository.save(agent);
        
        LiveChat savedChat = liveChatRepository.save(chat);
        
        // Send to Kafka
        kafkaTemplate.send("chat-assigned", savedChat);
        
        return savedChat;
    }
    
    public LiveChat acceptChat(String chatId, String agentId) {
        LiveChat chat = liveChatRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        
        if (!chat.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("Chat is not assigned to this agent");
        }
        
        if (chat.getStatus() != LiveChat.ChatStatus.ASSIGNED) {
            throw new IllegalStateException("Chat is not in assigned status");
        }
        
        chat.acceptChat();
        LiveChat savedChat = liveChatRepository.save(chat);
        
        // Send to Kafka
        kafkaTemplate.send("chat-accepted", savedChat);
        
        return savedChat;
    }
    
    public LiveChat transferChat(String chatId, String fromAgentId, String toAgentId) {
        LiveChat chat = liveChatRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        
        if (!chat.getAgentId().equals(fromAgentId)) {
            throw new IllegalArgumentException("Chat is not assigned to this agent");
        }
        
        User toAgent = userRepository.findById(toAgentId)
            .orElseThrow(() -> new IllegalArgumentException("Target agent not found"));
        
        if (toAgent.getStatus() != AgentStatus.AVAILABLE) {
            throw new IllegalStateException("Target agent is not available");
        }
        
        // Transfer chat
        chat.transferChat(toAgentId, toAgent.getFullName());
        
        // Update agent statuses
        User fromAgent = userRepository.findById(fromAgentId).orElse(null);
        if (fromAgent != null) {
            // Check if fromAgent has other active chats
            List<LiveChat> otherActiveChats = liveChatRepository.findActiveChatsByAgent(fromAgentId);
            if (otherActiveChats.size() <= 1) { // Only this chat
                fromAgent.setStatus(AgentStatus.AVAILABLE);
                userRepository.save(fromAgent);
            }
        }
        
        toAgent.setStatus(AgentStatus.BUSY);
        userRepository.save(toAgent);
        
        LiveChat savedChat = liveChatRepository.save(chat);
        
        // Send to Kafka
        kafkaTemplate.send("chat-transferred", savedChat);
        
        return savedChat;
    }
    
    public LiveChat endChat(String chatId, String agentId) {
        LiveChat chat = liveChatRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        
        if (!chat.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("Chat is not assigned to this agent");
        }
        
        chat.endChat();
        LiveChat savedChat = liveChatRepository.save(chat);
        
        // Update agent status
        User agent = userRepository.findById(agentId).orElse(null);
        if (agent != null) {
            // Check if agent has other active chats
            List<LiveChat> otherActiveChats = liveChatRepository.findActiveChatsByAgent(agentId);
            if (otherActiveChats.size() <= 1) { // Only this chat
                agent.setStatus(AgentStatus.AVAILABLE);
                userRepository.save(agent);
            }
        }
        
        // Send to Kafka
        kafkaTemplate.send("chat-ended", savedChat);
        
        return savedChat;
    }
    
    public Optional<LiveChat> getChatById(String chatId) {
        return liveChatRepository.findById(chatId);
    }
    
    public List<LiveChat> getChatsByTenant(String tenantId) {
        return liveChatRepository.findByTenantId(tenantId);
    }
    
    public List<LiveChat> getChatsByAgent(String agentId) {
        return liveChatRepository.findByAgentId(agentId);
    }
    
    public List<LiveChat> getActiveChatsByAgent(String agentId) {
        return liveChatRepository.findActiveChatsByAgent(agentId);
    }
    
    public List<LiveChat> getPendingChats(String tenantId) {
        return liveChatRepository.findPendingChats(tenantId);
    }
    
    public List<LiveChat> getPendingChatsByQueue(String tenantId, String queueId) {
        return liveChatRepository.findPendingChatsByQueue(tenantId, queueId);
    }
    
    public List<LiveChat> getPendingChatsByCampaign(String tenantId, String campaignId) {
        return liveChatRepository.findPendingChatsByCampaign(tenantId, campaignId);
    }
    
    public List<LiveChat> getOverdueAssignedChats(String tenantId) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5 minutes threshold
        return liveChatRepository.findOverdueAssignedChats(tenantId, threshold);
    }
    
    private boolean shouldAutoAssign(LiveChat chat) {
        // Check if campaign has auto-assign enabled
        // This would require CampaignService integration
        return true; // Simplified for now
    }
    
    private void assignChatToAvailableAgent(String chatId) {
        LiveChat chat = liveChatRepository.findById(chatId).orElse(null);
        if (chat == null) return;
        
        // Find available agents for the queue/campaign
        List<User> availableAgents = userRepository.findAvailableAgentsForQueue(chat.getTenantId(), chat.getQueueId());
        
        if (!availableAgents.isEmpty()) {
            // Simple round-robin assignment
            User selectedAgent = availableAgents.get(new Random().nextInt(availableAgents.size()));
            assignChatToAgent(chatId, selectedAgent.getId());
        }
    }
    
    private int getMaxConcurrentChats(User agent) {
        // This would come from agent's campaign settings
        return 3; // Default value
    }
}
