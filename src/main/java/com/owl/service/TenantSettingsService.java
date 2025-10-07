package com.owl.service;

import com.owl.model.TenantSettings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class TenantSettingsService {
    private final MongoTemplate core;

    public TenantSettingsService(MongoTemplate core) { this.core = core; }

    public TenantSettings getOrCreate(String tenantId) {
        TenantSettings s = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), TenantSettings.class);
        if (s == null) { s = new TenantSettings(tenantId); core.save(s); }
        return s;
    }

    public void setFallbackEnabled(String tenantId, boolean enabled) {
        TenantSettings s = getOrCreate(tenantId); s.setFallbackEnabled(enabled); core.save(s);
    }

    public void setGuardrailsEnabled(String tenantId, boolean enabled) {
        TenantSettings s = getOrCreate(tenantId); s.setGuardrailsEnabled(enabled); core.save(s);
    }
}

