package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class TenantKeyService {
    private final MongoTemplate core;
    private final boolean enabled;

    public TenantKeyService(MongoTemplate core, @Value("${owl.encryption.enabled:false}") boolean enabled) {
        this.core = core;
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    public String getOrCreateKey(String tenantId) {
        if (!enabled) return null;
        Map doc = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), Map.class, "tenant_keys");
        if (doc != null && doc.get("key") != null) return doc.get("key").toString();
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        core.save(Map.of("tenantId", tenantId, "key", b64), "tenant_keys");
        return b64;
    }
}

