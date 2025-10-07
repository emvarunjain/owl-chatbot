package com.owl.service;

import com.owl.model.ModelCredentials;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ModelCredentialsService {
    private final MongoTemplate core;
    public ModelCredentialsService(MongoTemplate core) { this.core = core; }

    public ModelCredentials get(String tenantId, String provider) {
        return core.findOne(Query.query(Criteria.where("tenantId").is(tenantId).and("provider").is(provider)), ModelCredentials.class);
    }

    public void set(ModelCredentials mc) { core.save(mc); }
}

