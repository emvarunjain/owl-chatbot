package com.owl.repo;

import com.owl.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    List<ChatMessage> findByChatId(String chatId);
    
    List<ChatMessage> findByChatIdOrderBySentAtAsc(String chatId);
    
    List<ChatMessage> findByChatIdAndSenderType(String chatId, ChatMessage.MessageType senderType);
    
    @Query("{ 'chatId': ?0, 'status': { $in: ['SENT', 'DELIVERED'] } }")
    List<ChatMessage> findUnseenMessages(String chatId);
    
    @Query("{ 'chatId': ?0, 'senderType': ?1, 'isTyping': true }")
    List<ChatMessage> findTypingIndicators(String chatId, ChatMessage.MessageType senderType);
    
    @Query("{ 'chatId': ?0, 'sentAt': { $gte: ?1 } }")
    List<ChatMessage> findMessagesAfter(String chatId, LocalDateTime after);
    
    @Query("{ 'chatId': ?0, 'sentAt': { $gte: ?1, $lte: ?2 } }")
    List<ChatMessage> findMessagesBetween(String chatId, LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'senderId': ?0, 'sentAt': { $gte: ?1, $lte: ?2 } }")
    List<ChatMessage> findMessagesBySenderAndDateRange(String senderId, LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'chatId': ?0, 'status': 'SENT' }")
    List<ChatMessage> findUnsentMessages(String chatId);
    
    @Query("{ 'chatId': ?0, 'status': 'DELIVERED', 'seenAt': null }")
    List<ChatMessage> findDeliveredButUnseenMessages(String chatId);
}
