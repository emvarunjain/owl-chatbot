package com.owl.repo;

import com.owl.model.Queue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRepository extends MongoRepository<Queue, String> {
    
    List<Queue> findByTenantId(String tenantId);
    
    List<Queue> findByTenantIdAndIsActive(String tenantId, boolean isActive);
    
    @Query("{ 'tenantId': ?0, 'agentIds': { $in: [?1] }, 'isActive': true }")
    List<Queue> findActiveQueuesByAgent(String tenantId, String agentId);
    
    @Query("{ 'tenantId': ?0, 'campaignIds': { $in: [?1] }, 'isActive': true }")
    List<Queue> findActiveQueuesByCampaign(String tenantId, String campaignId);
    
    @Query("{ 'tenantId': ?0, 'createdBy': ?1 }")
    List<Queue> findByTenantAndCreatedBy(String tenantId, String createdBy);
    
    Optional<Queue> findByTenantIdAndName(String tenantId, String name);
    
    boolean existsByTenantIdAndName(String tenantId, String name);
}
