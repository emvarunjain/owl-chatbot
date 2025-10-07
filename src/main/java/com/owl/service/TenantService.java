package com.owl.service;

import com.owl.model.Tenant;
import com.owl.repo.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

@Service
public class TenantService {

    private final TenantRepository repo;
    private final TenantProvisioningService provisioning;
    private final EventPublisher events;
    private final TenantSettingsService settings;

    public TenantService(TenantRepository repo,
                         TenantProvisioningService provisioning,
                         EventPublisher events,
                         TenantSettingsService settings) {
        this.repo = repo;
        this.provisioning = provisioning;
        this.events = events;
        this.settings = settings;
    }

    public Tenant create(String tenantId, String name) {
        if (repo.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException("tenantId already exists: " + tenantId);
        }
        Tenant saved = repo.save(new Tenant(tenantId, name));
        // one-call provisioning: prep tenant DB indexes
        provisioning.provision(tenantId);
        settings.getOrCreate(tenantId);
        events.tenantCreated(tenantId, name);
        return saved;
    }

    public Tenant get(String tenantId) {
        return repo.findByTenantId(tenantId).orElseThrow(
                () -> new NoSuchElementException("Tenant not found: " + tenantId));
    }

    public Tenant update(String tenantId, String name) {
        Tenant t = get(tenantId);
        t.setName(name);
        t.setUpdatedAt(OffsetDateTime.now());
        return repo.save(t);
    }

    public void delete(String tenantId) {
        if (!repo.existsByTenantId(tenantId)) {
            throw new NoSuchElementException("Tenant not found: " + tenantId);
        }
        repo.deleteByTenantId(tenantId);
    }

    public Page<Tenant> list(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }
}
