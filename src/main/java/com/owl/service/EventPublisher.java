package com.owl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Optional event publisher (Redpanda/Kafka). No-ops if KafkaTemplate is unavailable.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired(required = false)
    @Lazy
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private boolean kafkaAvailable() { return kafkaTemplate != null; }

    public void tenantCreated(String tenantId, String name) {
        send("owl.events.tenant", Map.of("type", "TENANT_CREATED", "tenantId", tenantId, "name", name));
    }

    public void ingested(String tenantId, String source, int chunks) {
        send("owl.events.ingest", Map.of("type", "INGESTED", "tenantId", tenantId, "source", source, "chunks", chunks));
    }

    public void chat(String tenantId, String question, boolean cacheHit) {
        send("owl.events.chat", Map.of("type", "CHAT", "tenantId", tenantId, "cacheHit", cacheHit, "q", question));
    }

    public void audit(String tenantId, String actor, String action, Map<String, Object> details) {
        send("owl.events.audit", Map.of(
                "type", "AUDIT",
                "tenantId", tenantId,
                "actor", actor,
                "action", action,
                "details", details == null ? Map.of() : details
        ));
    }

    public void feedback(String tenantId, String chatId, int rating) {
        send("owl.events.feedback", Map.of(
                "type", "FEEDBACK",
                "tenantId", tenantId,
                "chatId", chatId,
                "rating", rating
        ));
    }

    public void cost(String tenantId, double usd, long ms) {
        send("owl.events.cost", Map.of(
                "type", "COST",
                "tenantId", tenantId,
                "usd", usd,
                "latencyMs", ms
        ));
    }

    private void send(String topic, Object payload) {
        if (!kafkaAvailable()) {
            log.debug("Kafka unavailable; skipping event {}", payload);
            return;
        }
        try {
            String json = mapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            log.warn("Failed to publish event to {}: {}", topic, e.getMessage());
        }
    }
}
