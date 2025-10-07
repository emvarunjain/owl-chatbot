package com.owl.service;

import com.owl.model.LiveChat;
import com.owl.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaMessageHandler {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @KafkaListener(topics = "chat-created", groupId = "owl-chatbot")
    public void handleChatCreated(LiveChat chat) {
        // Notify agents about new chat
        messagingTemplate.convertAndSend("/topic/agents/chat-created", chat);
        
        // Notify specific queue agents
        if (chat.getQueueId() != null) {
            messagingTemplate.convertAndSend("/topic/queue/" + chat.getQueueId() + "/new-chat", chat);
        }
    }
    
    @KafkaListener(topics = "chat-assigned", groupId = "owl-chatbot")
    public void handleChatAssigned(LiveChat chat) {
        // Notify the assigned agent
        messagingTemplate.convertAndSend("/user/" + chat.getAgentId() + "/queue/chat-assigned", chat);
        
        // Notify other agents in the queue
        if (chat.getQueueId() != null) {
            messagingTemplate.convertAndSend("/topic/queue/" + chat.getQueueId() + "/chat-assigned", chat);
        }
    }
    
    @KafkaListener(topics = "chat-accepted", groupId = "owl-chatbot")
    public void handleChatAccepted(LiveChat chat) {
        // Notify customer that chat is accepted
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId() + "/accepted", chat);
        
        // Notify other agents
        if (chat.getQueueId() != null) {
            messagingTemplate.convertAndSend("/topic/queue/" + chat.getQueueId() + "/chat-accepted", chat);
        }
    }
    
    @KafkaListener(topics = "chat-transferred", groupId = "owl-chatbot")
    public void handleChatTransferred(LiveChat chat) {
        // Notify the new agent
        messagingTemplate.convertAndSend("/user/" + chat.getAgentId() + "/queue/chat-transferred", chat);
        
        // Notify all participants in the chat
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId() + "/transferred", chat);
    }
    
    @KafkaListener(topics = "chat-ended", groupId = "owl-chatbot")
    public void handleChatEnded(LiveChat chat) {
        // Notify all participants that chat ended
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId() + "/ended", chat);
        
        // Notify queue agents
        if (chat.getQueueId() != null) {
            messagingTemplate.convertAndSend("/topic/queue/" + chat.getQueueId() + "/chat-ended", chat);
        }
    }
    
    @KafkaListener(topics = "message-sent", groupId = "owl-chatbot")
    public void handleMessageSent(ChatMessage message) {
        // Send message to chat participants
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId() + "/message", message);
        
        // Update message status to delivered
        message.markAsDelivered();
    }
    
    @KafkaListener(topics = "message-delivered", groupId = "owl-chatbot")
    public void handleMessageDelivered(ChatMessage message) {
        // Notify sender that message was delivered
        messagingTemplate.convertAndSend("/user/" + message.getSenderId() + "/queue/message-delivered", message);
    }
    
    @KafkaListener(topics = "message-seen", groupId = "owl-chatbot")
    public void handleMessageSeen(ChatMessage message) {
        // Notify sender that message was seen
        messagingTemplate.convertAndSend("/user/" + message.getSenderId() + "/queue/message-seen", message);
    }
    
    @KafkaListener(topics = "typing-started", groupId = "owl-chatbot")
    public void handleTypingStarted(ChatMessage message) {
        // Send typing indicator to chat participants
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId() + "/typing", message);
    }
    
    @KafkaListener(topics = "typing-stopped", groupId = "owl-chatbot")
    public void handleTypingStopped(String payload) {
        // Parse payload: "chatId:senderId"
        String[] parts = payload.split(":");
        if (parts.length == 2) {
            String chatId = parts[0];
            String senderId = parts[1];
            
            Map<String, String> stopTyping = Map.of(
                "type", "typing_stopped",
                "senderId", senderId,
                "chatId", chatId
            );
            
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/typing", stopTyping);
        }
    }
    
    @KafkaListener(topics = "message-retry", groupId = "owl-chatbot")
    public void handleMessageRetry(ChatMessage message) {
        // Retry sending the message
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId() + "/message", message);
    }
    
    @KafkaListener(topics = "message-deleted", groupId = "owl-chatbot")
    public void handleMessageDeleted(String messageId) {
        // Notify chat participants that message was deleted
        Map<String, String> deleteNotification = Map.of(
            "type", "message_deleted",
            "messageId", messageId
        );
        
        // This would require looking up the chatId from messageId
        // For now, we'll send to a general topic
        messagingTemplate.convertAndSend("/topic/messages/deleted", deleteNotification);
    }
}
