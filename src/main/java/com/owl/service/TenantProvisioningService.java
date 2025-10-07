package com.owl.service;

import com.owl.model.ChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Service;

/**
 * One-call tenant provisioning: prepare per-tenant DB with minimal indexes.
 */
@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

    private final TenantMongoManager tenantMongoManager;
    private final QdrantTenantCollections qdrantTenants;

    public TenantProvisioningService(TenantMongoManager tenantMongoManager, QdrantTenantCollections qdrantTenants) {
        this.tenantMongoManager = tenantMongoManager;
        this.qdrantTenants = qdrantTenants;
    }

    public void provision(String tenantId) {
        MongoTemplate tpl = tenantMongoManager.templateForTenant(tenantId);
        IndexOperations ops = tpl.indexOps(ChatRecord.class);
        try { ops.ensureIndex(new Index().on("tenantId", Sort.Direction.ASC).named("idx_tenant")); } catch (Exception ignored) {}
        try { ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_createdAt")); } catch (Exception ignored) {}
        log.info("Provisioned tenant {} in DB {}", tenantId, tenantMongoManager.dbName(tenantId));
        // Optional stronger isolation: collection-per-tenant in Qdrant
        qdrantTenants.ensureTenantCollection(tenantId);
    }
}
