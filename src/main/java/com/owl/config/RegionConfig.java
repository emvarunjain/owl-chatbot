package com.owl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RegionConfig {
    private final Map<String, String> mongoUris = new HashMap<>();
    private final Map<String, String> qdrantUrls = new HashMap<>();

    public RegionConfig(
            @Value("${owl.regions.us-east-1.mongoUri:}") String use1Mongo,
            @Value("${owl.regions.us-east-1.qdrantUrl:}") String use1Qdrant,
            @Value("${owl.regions.eu-west-1.mongoUri:}") String euw1Mongo,
            @Value("${owl.regions.eu-west-1.qdrantUrl:}") String euw1Qdrant
    ) {
        if (!use1Mongo.isBlank()) mongoUris.put("us-east-1", use1Mongo);
        if (!use1Qdrant.isBlank()) qdrantUrls.put("us-east-1", use1Qdrant);
        if (!euw1Mongo.isBlank()) mongoUris.put("eu-west-1", euw1Mongo);
        if (!euw1Qdrant.isBlank()) qdrantUrls.put("eu-west-1", euw1Qdrant);
    }

    public String mongoUri(String region, String fallback) {
        return mongoUris.getOrDefault(region, fallback);
    }
    public String qdrantUrl(String region, String fallback) {
        return qdrantUrls.getOrDefault(region, fallback);
    }
}

