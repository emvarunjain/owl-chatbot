package com.owl.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMetricsService {
    private static class C {
        volatile long hits; volatile long missesOk; volatile long missesEmpty;
        volatile long modelMs;
    }
    private final Map<String, C> byTenant = new ConcurrentHashMap<>();

    public void incHit(String tenantId) { byTenant.computeIfAbsent(tenantId, t -> new C()).hits++; }
    public void incMissOk(String tenantId) { byTenant.computeIfAbsent(tenantId, t -> new C()).missesOk++; }
    public void incMissEmpty(String tenantId) { byTenant.computeIfAbsent(tenantId, t -> new C()).missesEmpty++; }
    public void addModelMs(String tenantId, long ms) { byTenant.computeIfAbsent(tenantId, t -> new C()).modelMs += ms; }

    public Map<String, Object> snapshot(String tenantId) {
        C c = byTenant.getOrDefault(tenantId, new C());
        long total = c.hits + c.missesOk + c.missesEmpty;
        double hitRatio = total == 0 ? 0.0 : ((double) c.hits) / total;
        return Map.of(
                "tenantId", tenantId,
                "hits", c.hits,
                "missesOk", c.missesOk,
                "missesEmpty", c.missesEmpty,
                "total", total,
                "hitRatio", hitRatio,
                "modelTimeMsTotal", c.modelMs
        );
    }
}

