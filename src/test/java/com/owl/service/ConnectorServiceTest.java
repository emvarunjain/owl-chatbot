package com.owl.service;

import com.owl.model.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectorServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private ConnectorSyncWorker connectorSyncWorker;

    private ConnectorService connectorService;

    @BeforeEach
    void setUp() {
        connectorService = new ConnectorService(mongoTemplate, eventPublisher, connectorSyncWorker);
    }

    @Test
    void create_shouldCreateNewConnector() {
        // Given
        String tenantId = "test-tenant";
        String type = "gdrive";
        Map<String, Object> config = Map.of("folderId", "123", "apiKey", "secret");
        
        when(mongoTemplate.save(any(ConnectorConfig.class))).thenAnswer(invocation -> {
            ConnectorConfig cc = invocation.getArgument(0);
            // Use reflection to set the ID since there's no setId method
            try {
                java.lang.reflect.Field idField = ConnectorConfig.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(cc, "connector-123");
            } catch (Exception e) {
                // Ignore reflection errors in test
            }
            return cc;
        });

        // When
        ConnectorConfig result = connectorService.create(tenantId, type, config);

        // Then
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals(type, result.getType());
        assertEquals(config, result.getConfig());
        assertEquals("created", result.getStatus());
        
        verify(mongoTemplate).save(any(ConnectorConfig.class));
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_CREATE"), any(Map.class));
    }

    @Test
    void list_shouldReturnConnectorsForTenant() {
        // Given
        String tenantId = "test-tenant";
        List<ConnectorConfig> expectedConnectors = List.of(
            createConnectorConfig("connector-1", tenantId, "gdrive"),
            createConnectorConfig("connector-2", tenantId, "sharepoint")
        );
        
        when(mongoTemplate.find(any(Query.class), eq(ConnectorConfig.class)))
            .thenReturn(expectedConnectors);

        // When
        List<ConnectorConfig> result = connectorService.list(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedConnectors, result);
        
        verify(mongoTemplate).find(any(Query.class), eq(ConnectorConfig.class));
    }

    @Test
    void delete_shouldDeleteConnector_whenConnectorExistsAndBelongsToTenant() {
        // Given
        String tenantId = "test-tenant";
        String connectorId = "connector-123";
        ConnectorConfig connector = createConnectorConfig(connectorId, tenantId, "gdrive");
        
        when(mongoTemplate.findById(connectorId, ConnectorConfig.class)).thenReturn(connector);

        // When
        connectorService.delete(tenantId, connectorId);

        // Then
        verify(mongoTemplate).findById(connectorId, ConnectorConfig.class);
        verify(mongoTemplate).remove(connector);
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_DELETE"), any(Map.class));
    }

    @Test
    void delete_shouldNotDeleteConnector_whenConnectorDoesNotExist() {
        // Given
        String tenantId = "test-tenant";
        String connectorId = "non-existent-connector";
        
        when(mongoTemplate.findById(connectorId, ConnectorConfig.class)).thenReturn(null);

        // When
        connectorService.delete(tenantId, connectorId);

        // Then
        verify(mongoTemplate).findById(connectorId, ConnectorConfig.class);
        verify(mongoTemplate, never()).remove(any(ConnectorConfig.class));
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_DELETE"), any(Map.class));
    }

    @Test
    void delete_shouldNotDeleteConnector_whenConnectorBelongsToDifferentTenant() {
        // Given
        String tenantId = "test-tenant";
        String connectorId = "connector-123";
        ConnectorConfig connector = createConnectorConfig(connectorId, "other-tenant", "gdrive");
        
        when(mongoTemplate.findById(connectorId, ConnectorConfig.class)).thenReturn(connector);

        // When
        connectorService.delete(tenantId, connectorId);

        // Then
        verify(mongoTemplate).findById(connectorId, ConnectorConfig.class);
        verify(mongoTemplate, never()).remove(any(ConnectorConfig.class));
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_DELETE"), any(Map.class));
    }

    @Test
    void startSync_shouldStartSync_whenConnectorExistsAndBelongsToTenant() {
        // Given
        String tenantId = "test-tenant";
        String connectorId = "connector-123";
        ConnectorConfig connector = createConnectorConfig(connectorId, tenantId, "gdrive");
        
        when(mongoTemplate.findById(connectorId, ConnectorConfig.class)).thenReturn(connector);
        when(mongoTemplate.save(any(ConnectorConfig.class))).thenReturn(connector);

        // When
        connectorService.startSync(tenantId, connectorId);

        // Then
        verify(mongoTemplate).findById(connectorId, ConnectorConfig.class);
        verify(mongoTemplate).save(any(ConnectorConfig.class));
        verify(connectorSyncWorker).sync(connector);
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_SYNC"), any(Map.class));
    }

    @Test
    void startSync_shouldNotStartSync_whenConnectorDoesNotExist() {
        // Given
        String tenantId = "test-tenant";
        String connectorId = "non-existent-connector";
        
        when(mongoTemplate.findById(connectorId, ConnectorConfig.class)).thenReturn(null);

        // When
        connectorService.startSync(tenantId, connectorId);

        // Then
        verify(mongoTemplate).findById(connectorId, ConnectorConfig.class);
        verify(mongoTemplate, never()).save(any(ConnectorConfig.class));
        verify(connectorSyncWorker, never()).sync(any(ConnectorConfig.class));
        verify(eventPublisher).audit(eq(tenantId), eq("user"), eq("CONNECTOR_SYNC"), any(Map.class));
    }

    private ConnectorConfig createConnectorConfig(String id, String tenantId, String type) {
        ConnectorConfig config = new ConnectorConfig();
        // Use reflection to set the ID since there's no setId method
        try {
            java.lang.reflect.Field idField = ConnectorConfig.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(config, id);
        } catch (Exception e) {
            // Ignore reflection errors in test
        }
        config.setTenantId(tenantId);
        config.setType(type);
        config.setConfig(Map.of("test", "value"));
        config.setStatus("created");
        return config;
    }
}
