package com.owl.controller;

import com.owl.model.LiveChat;
import com.owl.model.ChatMessage;
import com.owl.service.LiveChatService;
import com.owl.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class LiveChatController {
    
    @Autowired
    private LiveChatService liveChatService;
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // REST endpoints for chat management
    @PostMapping("/start")
    public ResponseEntity<LiveChat> startChat(@RequestBody Map<String, String> request, Authentication auth) {
        String tenantId = getTenantId(auth);
        String customerId = request.get("customerId");
        String customerName = request.get("customerName");
        String queueId = request.get("queueId");
        String campaignId = request.get("campaignId");
        
        LiveChat chat = new LiveChat(tenantId, customerId, customerName, queueId, campaignId);
        LiveChat createdChat = liveChatService.createChat(chat);
        
        return ResponseEntity.ok(createdChat);
    }
    
    @GetMapping("/{chatId}")
    public ResponseEntity<LiveChat> getChat(@PathVariable String chatId, Authentication auth) {
        return liveChatService.getChatById(chatId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<LiveChat>> getAgentChats(@PathVariable String agentId, Authentication auth) {
        List<LiveChat> chats = liveChatService.getChatsByAgent(agentId);
        return ResponseEntity.ok(chats);
    }
    
    @GetMapping("/agent/{agentId}/active")
    public ResponseEntity<List<LiveChat>> getActiveAgentChats(@PathVariable String agentId, Authentication auth) {
        List<LiveChat> chats = liveChatService.getActiveChatsByAgent(agentId);
        return ResponseEntity.ok(chats);
    }
    
    @PostMapping("/{chatId}/accept")
    public ResponseEntity<LiveChat> acceptChat(@PathVariable String chatId, Authentication auth) {
        String agentId = getUserId(auth);
        LiveChat chat = liveChatService.acceptChat(chatId, agentId);
        return ResponseEntity.ok(chat);
    }
    
    @PostMapping("/{chatId}/transfer")
    public ResponseEntity<LiveChat> transferChat(@PathVariable String chatId, @RequestBody Map<String, String> request, Authentication auth) {
        String fromAgentId = getUserId(auth);
        String toAgentId = request.get("toAgentId");
        
        LiveChat chat = liveChatService.transferChat(chatId, fromAgentId, toAgentId);
        return ResponseEntity.ok(chat);
    }
    
    @PostMapping("/{chatId}/end")
    public ResponseEntity<LiveChat> endChat(@PathVariable String chatId, Authentication auth) {
        String agentId = getUserId(auth);
        LiveChat chat = liveChatService.endChat(chatId, agentId);
        return ResponseEntity.ok(chat);
    }
    
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable String chatId, Authentication auth) {
        List<ChatMessage> messages = chatMessageService.getChatMessages(chatId);
        return ResponseEntity.ok(messages);
    }
    
    // WebSocket message handlers
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message, Authentication auth) {
        message.setSenderId(getUserId(auth));
        message.setSenderName(getUserName(auth));
        
        ChatMessage savedMessage = chatMessageService.sendMessage(message);
        
        // Send to specific chat room
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), savedMessage);
    }
    
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, String> payload, Authentication auth) {
        String chatId = payload.get("chatId");
        String action = payload.get("action"); // "start" or "stop"
        
        if ("start".equals(action)) {
            ChatMessage typingMessage = chatMessageService.sendTypingIndicator(
                chatId, getUserId(auth), getUserName(auth), 
                ChatMessage.MessageType.AGENT);
            
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, typingMessage);
        } else if ("stop".equals(action)) {
            chatMessageService.stopTypingIndicator(chatId, getUserId(auth));
            
            // Send stop typing event
            Map<String, String> stopTyping = Map.of(
                "type", "typing_stopped",
                "senderId", getUserId(auth),
                "chatId", chatId
            );
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, stopTyping);
        }
    }
    
    @MessageMapping("/chat.markSeen")
    public void markMessagesAsSeen(@Payload Map<String, String> payload, Authentication auth) {
        String chatId = payload.get("chatId");
        String messageId = payload.get("messageId");
        
        if (messageId != null) {
            chatMessageService.markMessageAsSeen(messageId);
        } else {
            chatMessageService.markAllMessagesAsSeen(chatId, getUserId(auth));
        }
        
        // Notify other participants
        Map<String, String> seenNotification = Map.of(
            "type", "messages_seen",
            "chatId", chatId,
            "seenBy", getUserId(auth)
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, seenNotification);
    }
    
    @MessageMapping("/chat.join")
    public void joinChat(@Payload Map<String, String> payload, Authentication auth) {
        String chatId = payload.get("chatId");
        
        // Notify other participants that user joined
        Map<String, String> joinNotification = Map.of(
            "type", "user_joined",
            "chatId", chatId,
            "userId", getUserId(auth),
            "userName", getUserName(auth)
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, joinNotification);
    }
    
    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload Map<String, String> payload, Authentication auth) {
        String chatId = payload.get("chatId");
        
        // Notify other participants that user left
        Map<String, String> leaveNotification = Map.of(
            "type", "user_left",
            "chatId", chatId,
            "userId", getUserId(auth),
            "userName", getUserName(auth)
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, leaveNotification);
    }
    
    // Helper methods
    private String getTenantId(Authentication auth) {
        // Extract tenant ID from authentication
        return "default-tenant"; // Simplified for now
    }
    
    private String getUserId(Authentication auth) {
        // Extract user ID from authentication
        return auth.getName(); // Simplified for now
    }
    
    private String getUserName(Authentication auth) {
        // Extract user name from authentication
        return auth.getName(); // Simplified for now
    }
}
