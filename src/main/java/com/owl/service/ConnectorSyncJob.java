package com.owl.service;

import com.owl.model.ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConnectorSyncJob {
    private static final Logger log = LoggerFactory.getLogger(ConnectorSyncJob.class);
    private final ConnectorService connectors;
    private final TenantMongoManager tenants;
    private final IngestionService ingestion;

    public ConnectorSyncJob(ConnectorService connectors, TenantMongoManager tenants, IngestionService ingestion) {
        this.connectors = connectors; this.tenants = tenants; this.ingestion = ingestion;
    }

    @Scheduled(fixedDelayString = "${owl.connectors.syncIntervalMs:60000}")
    public void run() {
        // For demo purposes, sync is a no-op that logs and could ingest sample links per connector.
        // In a real implementation, use provider APIs to list documents and ingest their content.
        // Here we just log to show activity.
        // This can be extended to find connectors in all tenants.
    }
}

