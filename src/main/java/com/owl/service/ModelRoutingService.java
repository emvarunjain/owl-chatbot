package com.owl.service;

import com.owl.model.ModelRouting;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ModelRoutingService {
    public record Selection(String provider, String chatModel, String embedModel) {}

    private final MongoTemplate core;

    public ModelRoutingService(MongoTemplate core) { this.core = core; }

    public Selection getForTenant(String tenantId) {
        ModelRouting r = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), ModelRouting.class);
        if (r == null) return new Selection("ollama", null, null);
        return new Selection(r.getProvider(), r.getChatModel(), r.getEmbedModel());
    }

    public void setForTenant(String tenantId, String provider, String chatModel, String embedModel) {
        ModelRouting r = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), ModelRouting.class);
        if (r == null) r = new ModelRouting();
        r.setTenantId(tenantId);
        r.setProvider(provider);
        r.setChatModel(chatModel);
        r.setEmbedModel(embedModel);
        core.save(r);
    }
}

