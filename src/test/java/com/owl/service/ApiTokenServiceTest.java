package com.owl.service;

import com.owl.model.ApiToken;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiTokenServiceTest {

    @Test
    void create_and_verify_and_revoke() {
        MongoTemplate core = mock(MongoTemplate.class);
        ApiTokenService svc = new ApiTokenService(core);

        ApiTokenService.Created created = svc.create("acme", "ci", List.of("admin:read"));
        assertNotNull(created.id());
        assertNotNull(created.token());

        // Stub verify lookup
        ApiToken saved = new ApiToken();
        saved.setTenantId("acme");
        saved.setName("ci");
        saved.setScopes(List.of("admin:read"));
        saved.setActive(true);
        when(core.findOne(any(Query.class), eq(ApiToken.class))).thenReturn(saved);

        Optional<ApiToken> v = svc.verify(created.token());
        assertTrue(v.isPresent());
        assertEquals("acme", v.get().getTenantId());

        // Revoke expects findById + save
        when(core.findById(anyString(), eq(ApiToken.class))).thenReturn(saved);
        assertDoesNotThrow(() -> svc.revoke("acme", "someId"));
    }
}

