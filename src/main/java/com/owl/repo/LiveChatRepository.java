package com.owl.repo;

import com.owl.model.LiveChat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveChatRepository extends MongoRepository<LiveChat, String> {
    
    List<LiveChat> findByTenantId(String tenantId);
    
    List<LiveChat> findByTenantIdAndStatus(String tenantId, LiveChat.ChatStatus status);
    
    List<LiveChat> findByAgentId(String agentId);
    
    List<LiveChat> findByAgentIdAndStatus(String agentId, LiveChat.ChatStatus status);
    
    List<LiveChat> findByQueueId(String queueId);
    
    List<LiveChat> findByCampaignId(String campaignId);
    
    @Query("{ 'tenantId': ?0, 'status': { $in: ['WAITING', 'ASSIGNED'] } }")
    List<LiveChat> findPendingChats(String tenantId);
    
    @Query("{ 'tenantId': ?0, 'queueId': ?1, 'status': { $in: ['WAITING', 'ASSIGNED'] } }")
    List<LiveChat> findPendingChatsByQueue(String tenantId, String queueId);
    
    @Query("{ 'tenantId': ?0, 'campaignId': ?1, 'status': { $in: ['WAITING', 'ASSIGNED'] } }")
    List<LiveChat> findPendingChatsByCampaign(String tenantId, String campaignId);
    
    @Query("{ 'agentId': ?0, 'status': 'ACTIVE' }")
    List<LiveChat> findActiveChatsByAgent(String agentId);
    
    @Query("{ 'tenantId': ?0, 'startedAt': { $gte: ?1, $lte: ?2 } }")
    List<LiveChat> findChatsByDateRange(String tenantId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'tenantId': ?0, 'agentId': ?1, 'startedAt': { $gte: ?2, $lte: ?3 } }")
    List<LiveChat> findAgentChatsByDateRange(String tenantId, String agentId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'tenantId': ?0, 'status': 'ACTIVE', 'assignedAt': { $lt: ?1 } }")
    List<LiveChat> findOverdueAssignedChats(String tenantId, LocalDateTime threshold);
    
    Optional<LiveChat> findByTenantIdAndCustomerIdAndStatus(String tenantId, String customerId, LiveChat.ChatStatus status);
}
