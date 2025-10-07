package com.owl.service;

import com.owl.model.Queue;
import com.owl.repo.QueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class QueueService {
    
    @Autowired
    private QueueRepository queueRepository;
    
    public Queue createQueue(Queue queue) {
        // Check if queue name already exists in tenant
        if (queueRepository.existsByTenantIdAndName(queue.getTenantId(), queue.getName())) {
            throw new IllegalArgumentException("Queue name already exists in this tenant");
        }
        
        queue.setCreatedAt(LocalDateTime.now());
        queue.setUpdatedAt(LocalDateTime.now());
        queue.setActive(true);
        
        return queueRepository.save(queue);
    }
    
    public Queue updateQueue(String queueId, Queue updatedQueue) {
        Queue existingQueue = queueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        // Update fields
        existingQueue.setName(updatedQueue.getName());
        existingQueue.setDescription(updatedQueue.getDescription());
        existingQueue.setActive(updatedQueue.isActive());
        existingQueue.setAgentIds(updatedQueue.getAgentIds());
        existingQueue.setCampaignIds(updatedQueue.getCampaignIds());
        existingQueue.setMaxWaitTime(updatedQueue.getMaxWaitTime());
        existingQueue.setMaxQueueSize(updatedQueue.getMaxQueueSize());
        existingQueue.setAllowTransfer(updatedQueue.isAllowTransfer());
        existingQueue.setTransferMessage(updatedQueue.getTransferMessage());
        existingQueue.updateTimestamp();
        
        return queueRepository.save(existingQueue);
    }
    
    public void deleteQueue(String queueId) {
        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        queue.setActive(false);
        queue.updateTimestamp();
        queueRepository.save(queue);
    }
    
    public Optional<Queue> getQueueById(String queueId) {
        return queueRepository.findById(queueId);
    }
    
    public List<Queue> getQueuesByTenant(String tenantId) {
        return queueRepository.findByTenantId(tenantId);
    }
    
    public List<Queue> getActiveQueuesByTenant(String tenantId) {
        return queueRepository.findByTenantIdAndIsActive(tenantId, true);
    }
    
    public List<Queue> getQueuesByAgent(String tenantId, String agentId) {
        return queueRepository.findActiveQueuesByAgent(tenantId, agentId);
    }
    
    public List<Queue> getQueuesByCampaign(String tenantId, String campaignId) {
        return queueRepository.findActiveQueuesByCampaign(tenantId, campaignId);
    }
    
    public Queue assignAgentsToQueue(String queueId, Set<String> agentIds) {
        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        queue.setAgentIds(agentIds);
        queue.updateTimestamp();
        
        return queueRepository.save(queue);
    }
    
    public Queue assignCampaignsToQueue(String queueId, Set<String> campaignIds) {
        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        queue.setCampaignIds(campaignIds);
        queue.updateTimestamp();
        
        return queueRepository.save(queue);
    }
    
    public Queue updateQueueSettings(String queueId, int maxWaitTime, int maxQueueSize, 
                                   boolean allowTransfer, String transferMessage) {
        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        queue.setMaxWaitTime(maxWaitTime);
        queue.setMaxQueueSize(maxQueueSize);
        queue.setAllowTransfer(allowTransfer);
        queue.setTransferMessage(transferMessage);
        queue.updateTimestamp();
        
        return queueRepository.save(queue);
    }
    
    public boolean isQueueActive(String queueId) {
        return queueRepository.findById(queueId)
            .map(Queue::isActive)
            .orElse(false);
    }
    
    public Optional<Queue> getQueueByName(String tenantId, String name) {
        return queueRepository.findByTenantIdAndName(tenantId, name);
    }
}
