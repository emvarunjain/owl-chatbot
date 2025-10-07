package com.owl.repo;

import com.owl.model.Campaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {
    
    List<Campaign> findByTenantId(String tenantId);
    
    List<Campaign> findByTenantIdAndIsActive(String tenantId, boolean isActive);
    
    @Query("{ 'tenantId': ?0, 'type': ?1, 'isActive': true }")
    List<Campaign> findActiveCampaignsByType(String tenantId, Campaign.CampaignType type);
    
    @Query("{ 'tenantId': ?0, 'agentIds': { $in: [?1] }, 'isActive': true }")
    List<Campaign> findActiveCampaignsByAgent(String tenantId, String agentId);
    
    @Query("{ 'tenantId': ?0, 'queueIds': { $in: [?1] }, 'isActive': true }")
    List<Campaign> findActiveCampaignsByQueue(String tenantId, String queueId);
    
    @Query("{ 'tenantId': ?0, 'createdBy': ?1 }")
    List<Campaign> findByTenantAndCreatedBy(String tenantId, String createdBy);
    
    Optional<Campaign> findByTenantIdAndName(String tenantId, String name);
    
    boolean existsByTenantIdAndName(String tenantId, String name);
}
