package com.owl.service;

import com.owl.model.Tenant;
import com.owl.repo.TenantRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantServiceTest {
    @Test
    void create_provisions_and_emits() {
        TenantRepository repo = mock(TenantRepository.class);
        TenantProvisioningService provision = mock(TenantProvisioningService.class);
        EventPublisher events = mock(EventPublisher.class);
        when(repo.existsByTenantId("acme")).thenReturn(false);
        when(repo.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantService svc = new TenantService(repo, provision, events, mock(TenantSettingsService.class));
        Tenant t = svc.create("acme", "Acme");
        assertEquals("acme", t.getTenantId());
        verify(provision).provision("acme");
        verify(events).tenantCreated("acme", "Acme");
    }

    @Test
    void create_throws_if_exists() {
        TenantRepository repo = mock(TenantRepository.class);
        when(repo.existsByTenantId("acme")).thenReturn(true);
        TenantService svc = new TenantService(repo, mock(TenantProvisioningService.class), mock(EventPublisher.class), mock(TenantSettingsService.class));
        assertThrows(IllegalArgumentException.class, () -> svc.create("acme", "Acme"));
    }

    @Test
    void get_update_delete() {
        TenantRepository repo = mock(TenantRepository.class);
        Tenant t = new Tenant("acme", "Acme"); t.setId("1");
        when(repo.findByTenantId("acme")).thenReturn(Optional.of(t));
        when(repo.existsByTenantId("acme")).thenReturn(true);
        when(repo.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        TenantService svc = new TenantService(repo, mock(TenantProvisioningService.class), mock(EventPublisher.class), mock(TenantSettingsService.class));

        assertEquals("Acme", svc.get("acme").getName());
        assertEquals("New", svc.update("acme", "New").getName());
        assertDoesNotThrow(() -> svc.delete("acme"));
    }
}
