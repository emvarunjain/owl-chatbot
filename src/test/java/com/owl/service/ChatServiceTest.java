package com.owl.service;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import com.owl.rerank.Reranker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    ChatClient chatClient;
    ChatClient.PromptRequestSpec prompt;
    ChatClient.ChatResponseSpec resp;
    DocumentRetrievalService retrieval;
    CacheService cache;
    ChatHistoryService history;
    EventPublisher events;
    Reranker reranker;
    ChatMetricsService chatMetrics;
    PreferenceService prefs;

    ChatService svc;
    BudgetService budgets;
    PromptCacheService promptCache;
    GuardrailsService guardrails;
    SafetyModelService safety;
    WebSearchService web;
    TenantSettingsService tenantSettings;
    QuotaService quotas;
    ModelRoutingService modelRouting;
    ModelProviderRouter modelRouter;

    @BeforeEach
    void setup() {
        chatClient = mock(ChatClient.class);
        prompt = mock(ChatClient.PromptRequestSpec.class);
        resp = mock(ChatClient.ChatResponseSpec.class);
        when(chatClient.prompt()).thenReturn(prompt);
        when(prompt.system(anyString())).thenReturn(prompt);
        when(prompt.user(anyString())).thenReturn(prompt);
        when(prompt.call()).thenReturn(resp);

        retrieval = mock(DocumentRetrievalService.class);
        cache = mock(CacheService.class);
        history = mock(ChatHistoryService.class);
        events = mock(EventPublisher.class);
        reranker = mock(Reranker.class);
        chatMetrics = new ChatMetricsService();
        prefs = mock(PreferenceService.class);
        when(prefs.lookup(anyString(), anyString())).thenReturn(java.util.Optional.empty());

        budgets = mock(BudgetService.class);
        when(budgets.allowSpend(anyString(), anyDouble())).thenReturn(true);
        promptCache = mock(PromptCacheService.class);
        safety = mock(SafetyModelService.class);
        when(safety.classify(anyString())).thenReturn("SAFE");
        guardrails = new GuardrailsService(false, safety);
        web = mock(WebSearchService.class);
        tenantSettings = mock(TenantSettingsService.class);
        when(tenantSettings.getOrCreate(anyString())).thenReturn(new com.owl.model.TenantSettings("acme"));
        quotas = mock(QuotaService.class);
        when(quotas.allowRequest(anyString())).thenReturn(true);
        doNothing().when(quotas).recordRequest(anyString());
        modelRouting = mock(ModelRoutingService.class);
        when(modelRouting.getForTenant(anyString())).thenReturn(new ModelRoutingService.Selection("ollama", null, null));
        modelRouter = mock(ModelProviderRouter.class);
        when(modelRouter.chatClientFor(anyString(), any())).thenReturn(chatClient);
        svc = new ChatService(
                chatClient, retrieval, cache, history, events,
                new SimpleMeterRegistry(), chatMetrics, prefs, budgets,
                promptCache, guardrails, web, tenantSettings, quotas, modelRouting, modelRouter,
                0.45, true, 0.0005, reranker);
    }

    @Test
    void cacheHit_shortCircuits() {
        when(cache.lookup("acme", "hello")).thenReturn(Optional.of("cached"));

        ChatResponse r = svc.answer(new ChatRequest("acme", "hello", false, null, null));

        assertEquals("cached", r.answer());
        assertTrue(r.sources().isEmpty());
        verify(history).save("acme", "hello", "cached", true, List.of());
        verify(events).chat("acme", "hello", true);
        verifyNoInteractions(retrieval);
        verify(prompt, never()).call();
    }

    @Test
    void kbMiss_withNoWeb_returnsIDK() {
        when(cache.lookup("acme", "question")).thenReturn(Optional.empty());
        when(retrieval.search(eq("acme"), eq("question"), any(), anyInt()))
                .thenReturn(List.of());

        ChatResponse r = svc.answer(new ChatRequest("acme", "question", false, null, null));

        assertTrue(r.answer().toLowerCase().contains("i don't know"));
        verify(cache).save(eq("acme"), eq("question"), contains("I don't know"));
        verify(history).save(eq("acme"), eq("question"), contains("I don't know"), eq(false), eq(List.of()));
        verify(events).chat("acme", "question", false);
        verify(prompt, never()).call();
    }

    @Test
    void grounded_withSources_callsModel_andCaches() {
        when(cache.lookup(anyString(), anyString())).thenReturn(Optional.empty());
        var d1 = new Document("chunk1", Map.of("filename", "doc1.pdf", "score", 0.95));
        var d2 = new Document("chunk2", Map.of("url", "https://x", "score", 0.92));
        var hits = List.of(new DocumentRetrievalService.Scored(d1, 0.95), new DocumentRetrievalService.Scored(d2, 0.92));
        when(retrieval.search(eq("acme"), eq("q"), any(), anyInt())).thenReturn(hits);
        when(reranker.rerank(anyString(), anyString(), anyList())).thenAnswer(inv -> inv.getArgument(2));
        when(resp.content()).thenReturn("Answer");

        ChatResponse r = svc.answer(new ChatRequest("acme", "q", false, null, null));

        assertTrue(r.answer().startsWith("Answer"));
        assertTrue(r.answer().contains("Sources:"));
        assertEquals(2, r.sources().size());
        verify(cache).save(eq("acme"), eq("q"), contains("Answer"));
        verify(history).save(eq("acme"), eq("q"), contains("Answer"), eq(false), anyList());
        verify(events).chat("acme", "q", false);
        verify(prompt).system(anyString());
        verify(prompt).user("q");
        verify(resp).content();
    }

    @Test
    void fallback_usesWeb_whenKbMiss_andEnabled() {
        when(cache.lookup("acme", "q2")).thenReturn(Optional.empty());
        when(prefs.lookup(anyString(), anyString())).thenReturn(Optional.empty());
        when(retrieval.search(eq("acme"), eq("q2"), any(), anyInt())).thenReturn(List.of());
        when(web.search(eq("q2"), anyInt())).thenReturn(List.of(new Document("web content", Map.of("url","https://ex"))));
        when(resp.content()).thenReturn("web-based answer");

        ChatRequest.FallbackPolicy fb = new ChatRequest.FallbackPolicy(true, null, 2);
        ChatResponse r = svc.answer(new ChatRequest("acme", "q2", false, null, fb));

        assertTrue(r.answer().contains("web-based answer"));
        verify(web).search(eq("q2"), anyInt());
        verify(prompt).system(anyString());
        verify(resp).content();
    }
}
