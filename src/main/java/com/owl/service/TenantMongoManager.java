package com.owl.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.owl.config.RegionConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides per-tenant MongoTemplate instances using a database-per-tenant strategy.
 * Includes region-aware DB naming when multi-region is enabled.
 */
@Component
public class TenantMongoManager {

    private final MongoClient mongoClient;
    private final Map<String, MongoTemplate> templates = new ConcurrentHashMap<>();
    private final TenantSettingsService settings;
    private final RegionConfig regions;

    public TenantMongoManager(MongoClient mongoClient, TenantSettingsService settings, RegionConfig regions) {
        this.mongoClient = mongoClient;
        this.settings = settings;
        this.regions = regions;
    }

    /** Returns a MongoTemplate bound to the tenant-specific database. */
    public MongoTemplate templateForTenant(String tenantId) {
        String region = resolveRegion(tenantId);
        String key = region + ":" + tenantId;
        return templates.computeIfAbsent(key, k -> {
            // If a per-region Mongo URI is configured, use a dedicated client; else use default
            String uri = regions.mongoUri(region, null);
            if (uri != null && !uri.isBlank()) {
                MongoClient regional = MongoClients.create(uri);
                return new MongoTemplate(regional, dbName(tenantId));
            }
            return new MongoTemplate(mongoClient, dbName(tenantId));
        });
    }

    public String dbName(String tenantId) {
        String region = resolveRegion(tenantId);
        if (region == null || region.isBlank()) region = "us-east-1";
        return ("owl_" + region + "_tenant_" + tenantId).replaceAll("[^a-zA-Z0-9_]+","_");
    }

    private String resolveRegion(String tenantId) {
        var s = settings.getOrCreate(tenantId);
        String header = TenantRegionContext.getOverrideRegion();
        return (header != null && !header.isBlank()) ? header : s.getRegion();
    }
}
