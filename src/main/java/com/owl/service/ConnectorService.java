package com.owl.service;

import com.owl.model.ConnectorConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConnectorService {
    private final MongoTemplate core;
    private final EventPublisher events;
    private final ConnectorSyncWorker worker;

    public ConnectorService(MongoTemplate core, EventPublisher events, ConnectorSyncWorker worker) { this.core = core; this.events = events; this.worker = worker; }

    public ConnectorConfig create(String tenantId, String type, Map<String, Object> config) {
        ConnectorConfig cc = new ConnectorConfig();
        cc.setTenantId(tenantId); cc.setType(type); cc.setConfig(config); cc.setStatus("created");
        core.save(cc);
        events.audit(tenantId, "user", "CONNECTOR_CREATE", Map.of("id", cc.getId(), "type", type));
        return cc;
    }

    public List<ConnectorConfig> list(String tenantId) {
        return core.find(Query.query(Criteria.where("tenantId").is(tenantId)), ConnectorConfig.class);
    }

    public void delete(String tenantId, String id) {
        ConnectorConfig cc = core.findById(id, ConnectorConfig.class);
        if (cc != null && cc.getTenantId().equals(tenantId)) core.remove(cc);
        events.audit(tenantId, "user", "CONNECTOR_DELETE", Map.of("id", id));
    }

    public void startSync(String tenantId, String id) {
        ConnectorConfig cc = core.findById(id, ConnectorConfig.class);
        if (cc == null || !cc.getTenantId().equals(tenantId)) return;
        cc.setStatus("running"); core.save(cc);
        events.audit(tenantId, "user", "CONNECTOR_SYNC", Map.of("id", id));
        worker.sync(cc);
    }
}
