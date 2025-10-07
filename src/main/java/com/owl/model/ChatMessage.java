package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    
    @Indexed
    private String chatId;
    
    private String senderId;
    private String senderName;
    private MessageType senderType; // CUSTOMER or AGENT
    private String content;
    private MessageType messageType;
    private MessageStatus status;
    
    // Timestamps
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime seenAt;
    
    // Message metadata
    private String attachmentUrl;
    private String attachmentType;
    private boolean isTyping;
    private String replyToMessageId; // For reply functionality
    
    public ChatMessage() {
        this.sentAt = LocalDateTime.now();
        this.status = MessageStatus.SENT;
        this.isTyping = false;
    }
    
    public ChatMessage(String chatId, String senderId, String senderName, MessageType senderType, String content) {
        this();
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderType = senderType;
        this.content = content;
        this.messageType = MessageType.TEXT;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public MessageType getSenderType() { return senderType; }
    public void setSenderType(MessageType senderType) { this.senderType = senderType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
    
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
    
    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    
    // Helper methods
    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    public void markAsSeen() {
        this.status = MessageStatus.SEEN;
        this.seenAt = LocalDateTime.now();
    }
    
    public void markAsFailed() {
        this.status = MessageStatus.FAILED;
    }
    
    public boolean isFromCustomer() {
        return senderType == MessageType.CUSTOMER;
    }
    
    public boolean isFromAgent() {
        return senderType == MessageType.AGENT;
    }
    
    public enum MessageType {
        CUSTOMER("Customer", "Message from customer"),
        AGENT("Agent", "Message from agent"),
        SYSTEM("System", "System message"),
        TEXT("Text", "Text message"),
        IMAGE("Image", "Image message"),
        FILE("File", "File attachment"),
        TYPING("Typing", "Typing indicator");
        
        private final String displayName;
        private final String description;
        
        MessageType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
