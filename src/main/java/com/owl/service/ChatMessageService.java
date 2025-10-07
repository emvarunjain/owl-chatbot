package com.owl.service;

import com.owl.model.ChatMessage;
import com.owl.model.LiveChat;
import com.owl.repo.ChatMessageRepository;
import com.owl.repo.LiveChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMessageService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private LiveChatRepository liveChatRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public ChatMessage sendMessage(ChatMessage message) {
        // Validate chat exists and is active
        LiveChat chat = liveChatRepository.findById(message.getChatId())
            .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        
        if (chat.getStatus() != LiveChat.ChatStatus.ACTIVE) {
            throw new IllegalStateException("Chat is not active");
        }
        
        // Set message properties
        message.setSentAt(LocalDateTime.now());
        message.setStatus(ChatMessage.MessageStatus.SENT);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update chat last message time
        chat.setLastMessageAt(LocalDateTime.now());
        chat.setMessageCount(chat.getMessageCount() + 1);
        liveChatRepository.save(chat);
        
        // Send to Kafka for real-time delivery
        kafkaTemplate.send("message-sent", savedMessage);
        
        return savedMessage;
    }
    
    public ChatMessage markMessageAsDelivered(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        message.markAsDelivered();
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Send to Kafka
        kafkaTemplate.send("message-delivered", savedMessage);
        
        return savedMessage;
    }
    
    public ChatMessage markMessageAsSeen(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        message.markAsSeen();
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Send to Kafka
        kafkaTemplate.send("message-seen", savedMessage);
        
        return savedMessage;
    }
    
    public void markAllMessagesAsSeen(String chatId, String userId) {
        List<ChatMessage> unseenMessages = chatMessageRepository.findDeliveredButUnseenMessages(chatId);
        
        for (ChatMessage message : unseenMessages) {
            if (!message.getSenderId().equals(userId)) { // Don't mark own messages as seen
                message.markAsSeen();
                chatMessageRepository.save(message);
            }
        }
    }
    
    public ChatMessage sendTypingIndicator(String chatId, String senderId, String senderName, 
                                         ChatMessage.MessageType senderType) {
        // Stop any existing typing indicators from this sender
        stopTypingIndicator(chatId, senderId);
        
        ChatMessage typingMessage = new ChatMessage();
        typingMessage.setChatId(chatId);
        typingMessage.setSenderId(senderId);
        typingMessage.setSenderName(senderName);
        typingMessage.setSenderType(senderType);
        typingMessage.setMessageType(ChatMessage.MessageType.TYPING);
        typingMessage.setContent("typing...");
        typingMessage.setTyping(true);
        typingMessage.setSentAt(LocalDateTime.now());
        typingMessage.setStatus(ChatMessage.MessageStatus.SENT);
        
        ChatMessage savedMessage = chatMessageRepository.save(typingMessage);
        
        // Send to Kafka
        kafkaTemplate.send("typing-started", savedMessage);
        
        return savedMessage;
    }
    
    public void stopTypingIndicator(String chatId, String senderId) {
        List<ChatMessage> typingMessages = chatMessageRepository.findTypingIndicators(chatId, 
            ChatMessage.MessageType.CUSTOMER.equals(senderId) ? 
                ChatMessage.MessageType.CUSTOMER : ChatMessage.MessageType.AGENT);
        
        for (ChatMessage message : typingMessages) {
            if (message.getSenderId().equals(senderId)) {
                chatMessageRepository.delete(message);
            }
        }
        
        // Send to Kafka
        kafkaTemplate.send("typing-stopped", chatId + ":" + senderId);
    }
    
    public List<ChatMessage> getChatMessages(String chatId) {
        return chatMessageRepository.findByChatIdOrderBySentAtAsc(chatId);
    }
    
    public List<ChatMessage> getChatMessagesAfter(String chatId, LocalDateTime after) {
        return chatMessageRepository.findMessagesAfter(chatId, after);
    }
    
    public List<ChatMessage> getUnseenMessages(String chatId) {
        return chatMessageRepository.findUnseenMessages(chatId);
    }
    
    public List<ChatMessage> getMessagesBySenderAndDateRange(String senderId, LocalDateTime start, LocalDateTime end) {
        return chatMessageRepository.findMessagesBySenderAndDateRange(senderId, start, end);
    }
    
    public Optional<ChatMessage> getMessageById(String messageId) {
        return chatMessageRepository.findById(messageId);
    }
    
    public void retryFailedMessages(String chatId) {
        List<ChatMessage> failedMessages = chatMessageRepository.findUnsentMessages(chatId);
        
        for (ChatMessage message : failedMessages) {
            message.setStatus(ChatMessage.MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            chatMessageRepository.save(message);
            
            // Send to Kafka for retry
            kafkaTemplate.send("message-retry", message);
        }
    }
    
    public void deleteMessage(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        chatMessageRepository.delete(message);
        
        // Send to Kafka
        kafkaTemplate.send("message-deleted", messageId);
    }
}
