package com.owl.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class QuotaService {
    private final MongoTemplate core;
    private final PlanService plans;

    public QuotaService(MongoTemplate core, PlanService plans) { this.core = core; this.plans = plans; }

    private String monthKeyNow() { return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")); }

    public boolean allowRequest(String tenantId) {
        String mk = monthKeyNow();
        Map doc = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId).and("month").is(mk)), Map.class, "usage_counters");
        int used = doc == null ? 0 : ((Number) doc.getOrDefault("requests", 0)).intValue();
        String planName = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), com.owl.model.TenantSettings.class).getPlan();
        com.owl.model.Plan plan = plans.getPlan(planName == null ? "free" : planName);
        int limit = plan == null ? 3000 : plan.getMonthlyRequests();
        return used < limit + (plan == null ? 0 : plan.getBurstCredits());
    }

    public void recordRequest(String tenantId) {
        String mk = monthKeyNow();
        Map doc = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId).and("month").is(mk)), Map.class, "usage_counters");
        int used = doc == null ? 0 : ((Number) doc.getOrDefault("requests", 0)).intValue();
        used++;
        core.save(Map.of("tenantId", tenantId, "month", mk, "requests", used), "usage_counters");
    }
}

