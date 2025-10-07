package com.owl.service;

import com.owl.model.Budget;
import com.owl.metrics.BudgetMetricsExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class BudgetService {
    private final MongoTemplate core;
    private final BudgetMetricsExporter exporter;
    private final boolean enabled;
    private final double defaultBudget;

    public BudgetService(MongoTemplate core, BudgetMetricsExporter exporter,
                         @Value("${owl.cost.budget.enabled:false}") boolean enabled,
                         @Value("${owl.cost.budget.defaultUsd:0}") double defaultBudget) {
        this.core = core;
        this.exporter = exporter;
        this.enabled = enabled;
        this.defaultBudget = defaultBudget;
    }

    private static String monthKeyNow() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public boolean allowSpend(String tenantId, double amountUsd) {
        if (!enabled) return true;
        Budget b = getOrCreate(tenantId);
        if (b.getMonthlyBudgetUsd() <= 0) return true; // unlimited
        return (b.getSpentUsdCurrentMonth() + amountUsd) <= b.getMonthlyBudgetUsd();
    }

    public void recordSpend(String tenantId, double amountUsd) {
        if (!enabled) return;
        Budget b = getOrCreate(tenantId);
        // rollover month
        String now = monthKeyNow();
        if (!now.equals(b.getMonthKey())) {
            b.setMonthKey(now);
            b.setSpentUsdCurrentMonth(0);
        }
        b.setSpentUsdCurrentMonth(b.getSpentUsdCurrentMonth() + amountUsd);
        core.save(b);
        exporter.update(tenantId, b.getMonthlyBudgetUsd(), b.getSpentUsdCurrentMonth());
    }

    public Budget getOrCreate(String tenantId) {
        String now = monthKeyNow();
        Budget b = core.findOne(Query.query(Criteria.where("tenantId").is(tenantId)), Budget.class);
        if (b == null) {
            b = new Budget(tenantId, defaultBudget, now);
            core.save(b);
        } else if (!now.equals(b.getMonthKey())) {
            b.setMonthKey(now); b.setSpentUsdCurrentMonth(0); core.save(b);
        }
        exporter.update(tenantId, b.getMonthlyBudgetUsd(), b.getSpentUsdCurrentMonth());
        return b;
    }

    public Map<String, Object> snapshot(String tenantId) {
        Budget b = getOrCreate(tenantId);
        return Map.of(
                "tenantId", tenantId,
                "monthlyBudgetUsd", b.getMonthlyBudgetUsd(),
                "spentUsdCurrentMonth", b.getSpentUsdCurrentMonth(),
                "monthKey", b.getMonthKey()
        );
    }

    public void setBudget(String tenantId, double monthlyBudgetUsd) {
        Budget b = getOrCreate(tenantId);
        b.setMonthlyBudgetUsd(monthlyBudgetUsd);
        core.save(b);
        exporter.update(tenantId, b.getMonthlyBudgetUsd(), b.getSpentUsdCurrentMonth());
    }
}
