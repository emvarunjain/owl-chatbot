package com.owl.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BudgetMetricsExporter {
    private final MeterRegistry registry;
    private static class Holder { volatile double v; Holder(double v){ this.v=v; } }
    private final Map<String, Holder> budget = new ConcurrentHashMap<>();
    private final Map<String, Holder> spent = new ConcurrentHashMap<>();

    public BudgetMetricsExporter(MeterRegistry registry) {
        this.registry = registry;
    }

    public void update(String tenantId, double budgetUsd, double spentUsd) {
        Holder b = budget.computeIfAbsent(tenantId, t -> {
            Holder h = new Holder(budgetUsd);
            registry.gauge("chat.budget.usd", Tags.of("tenantId", tenantId), h, x -> x.v);
            return h;
        });
        Holder s = spent.computeIfAbsent(tenantId, t -> {
            Holder h = new Holder(spentUsd);
            registry.gauge("chat.spend.usd", Tags.of("tenantId", tenantId), h, x -> x.v);
            return h;
        });
        b.v = budgetUsd;
        s.v = spentUsd;
    }
}

