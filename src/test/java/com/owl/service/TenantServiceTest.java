package com.owl.service;

import com.owl.model.Tenant;
import com.owl.repo.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private TenantProvisioningService provisioningService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private TenantSettingsService settingsService;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, provisioningService, eventPublisher, settingsService);
    }

    @Test
    void create_shouldCreateNewTenant_whenTenantIdDoesNotExist() {
        // Given
        String tenantId = "test-tenant";
        String name = "Test Tenant";
        Tenant savedTenant = new Tenant(tenantId, name);
        
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        // When
        Tenant result = tenantService.create(tenantId, name);

        // Then
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals(name, result.getName());
        
        verify(tenantRepository).existsByTenantId(tenantId);
        verify(tenantRepository).save(any(Tenant.class));
        verify(provisioningService).provision(tenantId);
        verify(settingsService).getOrCreate(tenantId);
        verify(eventPublisher).tenantCreated(tenantId, name);
    }

    @Test
    void create_shouldThrowException_whenTenantIdAlreadyExists() {
        // Given
        String tenantId = "existing-tenant";
        String name = "Existing Tenant";
        
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> tenantService.create(tenantId, name));
        
        assertEquals("tenantId already exists: " + tenantId, exception.getMessage());
        
        verify(tenantRepository).existsByTenantId(tenantId);
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(provisioningService, never()).provision(anyString());
    }

    @Test
    void get_shouldReturnTenant_whenTenantExists() {
        // Given
        String tenantId = "test-tenant";
        Tenant tenant = new Tenant(tenantId, "Test Tenant");
        
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        // When
        Tenant result = tenantService.get(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        verify(tenantRepository).findByTenantId(tenantId);
    }

    @Test
    void get_shouldThrowException_whenTenantDoesNotExist() {
        // Given
        String tenantId = "non-existent-tenant";
        
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () -> tenantService.get(tenantId));
        verify(tenantRepository).findByTenantId(tenantId);
    }

    @Test
    void update_shouldUpdateTenant_whenTenantExists() {
        // Given
        String tenantId = "test-tenant";
        String newName = "Updated Tenant Name";
        Tenant existingTenant = new Tenant(tenantId, "Old Name");
        Tenant updatedTenant = new Tenant(tenantId, newName);
        
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(updatedTenant);

        // When
        Tenant result = tenantService.update(tenantId, newName);

        // Then
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals(newName, result.getName());
        assertNotNull(result.getUpdatedAt());
        
        verify(tenantRepository).findByTenantId(tenantId);
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void delete_shouldDeleteTenant_whenTenantExists() {
        // Given
        String tenantId = "test-tenant";
        
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(true);

        // When
        tenantService.delete(tenantId);

        // Then
        verify(tenantRepository).existsByTenantId(tenantId);
        verify(tenantRepository).deleteByTenantId(tenantId);
    }

    @Test
    void delete_shouldThrowException_whenTenantDoesNotExist() {
        // Given
        String tenantId = "non-existent-tenant";
        
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(false);

        // When & Then
        assertThrows(Exception.class, () -> tenantService.delete(tenantId));
        verify(tenantRepository).existsByTenantId(tenantId);
        verify(tenantRepository, never()).deleteByTenantId(anyString());
    }
}