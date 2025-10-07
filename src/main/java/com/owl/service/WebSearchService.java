package com.owl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WebSearchService {
    private final boolean enabled;
    private final String provider; // serpapi|bing
    private final String serpApiKey;
    private final String bingKey;
    private final String bingEndpoint;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public WebSearchService(@Value("${owl.fallback.enabled:false}") boolean enabled,
                            @Value("${owl.fallback.provider:serpapi}") String provider,
                            @Value("${owl.fallback.serpapi.apiKey:}") String serpApiKey,
                            @Value("${owl.fallback.bing.apiKey:}") String bingKey,
                            @Value("${owl.fallback.bing.endpoint:https://api.bing.microsoft.com/v7.0/search}") String bingEndpoint) {
        this.enabled = enabled;
        this.provider = provider;
        this.serpApiKey = serpApiKey;
        this.bingKey = bingKey;
        this.bingEndpoint = bingEndpoint;
    }

    public List<Document> search(String query, int max) {
        if (!enabled) return List.of();
        try {
            if ("bing".equalsIgnoreCase(provider) && !bingKey.isBlank()) {
                return bingSearch(query, max);
            }
            if ("serpapi".equalsIgnoreCase(provider) && !serpApiKey.isBlank()) {
                return serpApiSearch(query, max);
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private List<Document> serpApiSearch(String query, int max) throws Exception {
        String url = "https://serpapi.com/search.json?engine=google&num=" + Math.max(1, max) +
                "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&api_key=" + URLEncoder.encode(serpApiKey, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) return List.of();
        JsonNode root = mapper.readTree(resp.body());
        JsonNode list = root.path("organic_results");
        List<Document> out = new ArrayList<>();
        int i = 0;
        for (JsonNode n : list) {
            if (i >= max) break;
            String title = n.path("title").asText("");
            String snippet = n.path("snippet").asText("");
            String link = n.path("link").asText("");
            String text = (title + " — " + snippet).trim();
            out.add(new Document(text, Map.of("url", link, "type", "web")));
            i++;
        }
        return out;
    }

    private List<Document> bingSearch(String query, int max) throws Exception {
        String url = bingEndpoint + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Ocp-Apim-Subscription-Key", bingKey)
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) return List.of();
        JsonNode root = mapper.readTree(resp.body());
        JsonNode list = root.path("webPages").path("value");
        List<Document> out = new ArrayList<>();
        int i = 0;
        for (JsonNode n : list) {
            if (i >= max) break;
            String title = n.path("name").asText("");
            String snippet = n.path("snippet").asText("");
            String link = n.path("url").asText("");
            String text = (title + " — " + snippet).trim();
            out.add(new Document(text, Map.of("url", link, "type", "web")));
            i++;
        }
        return out;
    }
}
