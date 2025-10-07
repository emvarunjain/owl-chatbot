package com.owl.service;

import com.owl.model.Plan;
import com.owl.model.TenantSettings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {
    private final MongoTemplate core;
    private final TenantSettingsService settings;

    public PlanService(MongoTemplate core, TenantSettingsService settings) {
        this.core = core;
        this.settings = settings;
        ensureDefaults();
    }

    private void ensureDefaults() {
        if (core.findOne(Query.query(Criteria.where("name").is("free")), Plan.class) == null) {
            core.save(new Plan("free", 3000, 300, 0.0, "99.0%"));
        }
        if (core.findOne(Query.query(Criteria.where("name").is("pro")), Plan.class) == null) {
            core.save(new Plan("pro", 100000, 5000, 50.0, "99.5%"));
        }
        if (core.findOne(Query.query(Criteria.where("name").is("enterprise")), Plan.class) == null) {
            core.save(new Plan("enterprise", 1000000, 20000, 0.0, "99.9%"));
        }
    }

    public List<Plan> list() {
        return core.findAll(Plan.class);
    }

    public void assign(String tenantId, String planName) {
        TenantSettings s = settings.getOrCreate(tenantId);
        s.setPlan(planName);
        core.save(s);
    }

    public Plan getPlan(String name) { return core.findOne(Query.query(Criteria.where("name").is(name)), Plan.class); }
}

