package com.owl.service;

import com.owl.model.ApiToken;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ApiTokenService {
    private final MongoTemplate core;
    private final SecureRandom rng = new SecureRandom();

    public ApiTokenService(MongoTemplate core) { this.core = core; }

    public record Created(String id, String token) {}

    public Created create(String tenantId, String name, List<String> scopes) {
        String token = randomToken();
        String hash = hash(token);
        ApiToken t = new ApiToken();
        t.setTenantId(tenantId);
        t.setName(name);
        t.setScopes(scopes);
        t.setTokenHash(hash);
        core.save(t);
        return new Created(t.getId(), token);
    }

    public Optional<ApiToken> verify(String token) {
        String hash = hash(token);
        ApiToken t = core.findOne(Query.query(Criteria.where("tokenHash").is(hash).and("active").is(true)), ApiToken.class);
        if (t == null) return Optional.empty();
        t.setLastUsedAt(OffsetDateTime.now());
        core.save(t);
        return Optional.of(t);
    }

    public List<ApiToken> list(String tenantId) {
        return core.find(Query.query(Criteria.where("tenantId").is(tenantId)), ApiToken.class);
    }

    public void revoke(String tenantId, String id) {
        ApiToken t = core.findById(id, ApiToken.class);
        if (t != null && t.getTenantId().equals(tenantId)) {
            t.setActive(false);
            core.save(t);
        }
    }

    private String randomToken() {
        byte[] b = new byte[32]; rng.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}

