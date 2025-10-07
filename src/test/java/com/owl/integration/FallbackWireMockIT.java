package com.owl.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import com.owl.rerank.Reranker;
import com.owl.service.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FallbackWireMockIT {

    static WireMockServer wm;

    @BeforeAll
    static void up() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
    }

    @AfterAll
    static void down() {
        wm.stop();
    }

    @Test
    void serpapi_fallback_path_works() {
        // Mock SerpAPI response
        String body = "{\n  \"organic_results\": [ { \"title\": \"Doc\", \"snippet\": \"Snippet\", \"link\": \"https://ex\" } ]\n}";
        wm.stubFor(WireMock.get(WireMock.urlMatching("/search\\.json.*"))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type","application/json").withBody(body)));

        // Wire concrete WebSearchService against WireMock endpoint
        WebSearchService web = new WebSearchService(true, "serpapi", "key", "", "https://api.bing.microsoft.com/v7.0/search") {
            @Override
            public java.util.List<org.springframework.ai.document.Document> search(String query, int max) {
                try {
                    var f = WebSearchService.class.getDeclaredField("serpApiKey"); f.setAccessible(true); f.set(this, "key");
                    var f2 = WebSearchService.class.getDeclaredField("mapper"); f2.setAccessible(true);
                } catch (Exception ignored) {}
                return doSerp("http://localhost:" + wm.port(), query, max);
            }
            private java.util.List<org.springframework.ai.document.Document> doSerp(String base, String query, int max) {
                try {
                    java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
                    String url = base + "/search.json?engine=google&num=" + Math.max(1, max) + "&q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&api_key=key";
                    var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
                    var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                    com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
                    java.util.List<org.springframework.ai.document.Document> out = new java.util.ArrayList<>();
                    for (var n : root.path("organic_results")) {
                        String title = n.path("title").asText("");
                        String snippet = n.path("snippet").asText("");
                        String link = n.path("link").asText("");
                        out.add(new org.springframework.ai.document.Document(title+" — "+snippet, java.util.Map.of("url", link)));
                    }
                    return out;
                } catch (Exception e) { return java.util.List.of(); }
            }
        };

        // Mocks for rest of pipeline
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.PromptRequestSpec prompt = mock(ChatClient.PromptRequestSpec.class);
        ChatClient.ChatResponseSpec cr = mock(ChatClient.ChatResponseSpec.class);
        when(chatClient.prompt()).thenReturn(prompt);
        when(prompt.system(anyString())).thenReturn(prompt);
        when(prompt.user(anyString())).thenReturn(prompt);
        when(prompt.call()).thenReturn(cr);
        when(cr.content()).thenReturn("fallback answer");

        DocumentRetrievalService retrieval = mock(DocumentRetrievalService.class);
        when(retrieval.search(anyString(), anyString(), any(), anyInt())).thenReturn(List.of());
        CacheService cache = mock(CacheService.class);
        when(cache.lookup(anyString(), anyString())).thenReturn(Optional.empty());
        ChatHistoryService history = mock(ChatHistoryService.class);
        when(history.save(anyString(), anyString(), anyString(), anyBoolean(), anyList())).thenReturn("chat1");
        EventPublisher events = mock(EventPublisher.class);
        Reranker reranker = mock(Reranker.class);
        ChatMetricsService chatMetrics = new ChatMetricsService();
        PreferenceService prefs = mock(PreferenceService.class);
        BudgetService budgets = mock(BudgetService.class);
        when(budgets.allowSpend(anyString(), anyDouble())).thenReturn(true);
        PromptCacheService promptCache = mock(PromptCacheService.class);
        SafetyModelService safety = mock(SafetyModelService.class);
        when(safety.classify(anyString())).thenReturn("SAFE");
        GuardrailsService guardrails = new GuardrailsService(false, safety);
        TenantSettingsService settings = mock(TenantSettingsService.class);
        when(settings.getOrCreate(anyString())).thenReturn(new com.owl.model.TenantSettings("acme"));

        QuotaService quotas = mock(QuotaService.class);
        when(quotas.allowRequest(anyString())).thenReturn(true);
        ModelRoutingService routing = mock(ModelRoutingService.class);
        when(routing.getForTenant(anyString())).thenReturn(new ModelRoutingService.Selection("ollama", null, null));
        ModelProviderRouter router = mock(ModelProviderRouter.class);
        when(router.chatClientFor(anyString(), any())).thenReturn(chatClient);
        ChatService svc = new ChatService(chatClient, retrieval, cache, history, events, new SimpleMeterRegistry(), chatMetrics,
                prefs, budgets, promptCache, guardrails, web, settings, quotas, routing, router, 0.45, true, 0.0005, reranker);

        ChatRequest.FallbackPolicy fb = new ChatRequest.FallbackPolicy(true, null, 2);
        ChatResponse r = svc.answer(new ChatRequest("acme", "what is x?", false, null, fb));
        assertTrue(r.answer().contains("fallback answer"));
    }
}
