package com.owl.service;

import com.owl.model.Campaign;
import com.owl.repo.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CampaignService {
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    public Campaign createCampaign(Campaign campaign) {
        // Check if campaign name already exists in tenant
        if (campaignRepository.existsByTenantIdAndName(campaign.getTenantId(), campaign.getName())) {
            throw new IllegalArgumentException("Campaign name already exists in this tenant");
        }
        
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setUpdatedAt(LocalDateTime.now());
        campaign.setActive(true);
        
        return campaignRepository.save(campaign);
    }
    
    public Campaign updateCampaign(String campaignId, Campaign updatedCampaign) {
        Campaign existingCampaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        
        // Update fields
        existingCampaign.setName(updatedCampaign.getName());
        existingCampaign.setDescription(updatedCampaign.getDescription());
        existingCampaign.setType(updatedCampaign.getType());
        existingCampaign.setActive(updatedCampaign.isActive());
        existingCampaign.setQueueIds(updatedCampaign.getQueueIds());
        existingCampaign.setAgentIds(updatedCampaign.getAgentIds());
        existingCampaign.setMaxConcurrentChats(updatedCampaign.getMaxConcurrentChats());
        existingCampaign.setResponseTimeoutSeconds(updatedCampaign.getResponseTimeoutSeconds());
        existingCampaign.setAutoAssign(updatedCampaign.isAutoAssign());
        existingCampaign.setWelcomeMessage(updatedCampaign.getWelcomeMessage());
        existingCampaign.updateTimestamp();
        
        return campaignRepository.save(existingCampaign);
    }
    
    public void deleteCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        
        campaign.setActive(false);
        campaign.updateTimestamp();
        campaignRepository.save(campaign);
    }
    
    public Optional<Campaign> getCampaignById(String campaignId) {
        return campaignRepository.findById(campaignId);
    }
    
    public List<Campaign> getCampaignsByTenant(String tenantId) {
        return campaignRepository.findByTenantId(tenantId);
    }
    
    public List<Campaign> getActiveCampaignsByTenant(String tenantId) {
        return campaignRepository.findByTenantIdAndIsActive(tenantId, true);
    }
    
    public List<Campaign> getCampaignsByType(String tenantId, Campaign.CampaignType type) {
        return campaignRepository.findActiveCampaignsByType(tenantId, type);
    }
    
    public List<Campaign> getCampaignsByAgent(String tenantId, String agentId) {
        return campaignRepository.findActiveCampaignsByAgent(tenantId, agentId);
    }
    
    public List<Campaign> getCampaignsByQueue(String tenantId, String queueId) {
        return campaignRepository.findActiveCampaignsByQueue(tenantId, queueId);
    }
    
    public Campaign assignAgentsToCampaign(String campaignId, Set<String> agentIds) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        
        campaign.setAgentIds(agentIds);
        campaign.updateTimestamp();
        
        return campaignRepository.save(campaign);
    }
    
    public Campaign assignQueuesToCampaign(String campaignId, Set<String> queueIds) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        
        campaign.setQueueIds(queueIds);
        campaign.updateTimestamp();
        
        return campaignRepository.save(campaign);
    }
    
    public Campaign updateCampaignSettings(String campaignId, int maxConcurrentChats, 
                                         int responseTimeoutSeconds, boolean autoAssign, 
                                         String welcomeMessage) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        
        campaign.setMaxConcurrentChats(maxConcurrentChats);
        campaign.setResponseTimeoutSeconds(responseTimeoutSeconds);
        campaign.setAutoAssign(autoAssign);
        campaign.setWelcomeMessage(welcomeMessage);
        campaign.updateTimestamp();
        
        return campaignRepository.save(campaign);
    }
    
    public boolean isCampaignActive(String campaignId) {
        return campaignRepository.findById(campaignId)
            .map(Campaign::isActive)
            .orElse(false);
    }
    
    public Optional<Campaign> getCampaignByName(String tenantId, String name) {
        return campaignRepository.findByTenantIdAndName(tenantId, name);
    }
}
