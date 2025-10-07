package com.owl.repo;

import com.owl.model.User;
import com.owl.model.UserRole;
import com.owl.model.AgentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByTenantId(String tenantId);
    
    List<User> findByTenantIdAndRole(String tenantId, UserRole role);
    
    List<User> findByTenantIdAndStatus(String tenantId, AgentStatus status);
    
    List<User> findByTenantIdAndIsActive(String tenantId, boolean isActive);
    
    @Query("{ 'tenantId': ?0, 'role': ?1, 'isActive': true }")
    List<User> findActiveUsersByTenantAndRole(String tenantId, UserRole role);
    
    @Query("{ 'tenantId': ?0, 'status': ?1, 'isActive': true }")
    List<User> findActiveUsersByTenantAndStatus(String tenantId, AgentStatus status);
    
    @Query("{ 'tenantId': ?0, 'queueIds': { $in: [?1] }, 'status': 'AVAILABLE', 'isActive': true }")
    List<User> findAvailableAgentsForQueue(String tenantId, String queueId);
    
    @Query("{ 'tenantId': ?0, 'campaignIds': { $in: [?1] }, 'status': 'AVAILABLE', 'isActive': true }")
    List<User> findAvailableAgentsForCampaign(String tenantId, String campaignId);
    
    @Query("{ 'tenantId': ?0, 'supervisorId': ?1, 'isActive': true }")
    List<User> findSubordinatesBySupervisor(String tenantId, String supervisorId);
    
    @Query("{ 'tenantId': ?0, 'subordinateIds': { $in: [?1] }, 'isActive': true }")
    List<User> findSupervisorsBySubordinate(String tenantId, String subordinateId);
    
    @Query("{ 'tenantId': ?0, 'permissions': { $in: [?1] }, 'isActive': true }")
    List<User> findUsersWithPermission(String tenantId, String permission);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByTenantIdAndUsername(String tenantId, String username);
    
    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
