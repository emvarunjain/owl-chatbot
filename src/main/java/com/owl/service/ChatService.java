package com.owl.service;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient; // <- IMPORTANT: correct package
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.owl.rerank.Reranker;

/**
 * Chat orchestration with guardrails + semantic cache (Spring AI 1.0.1).
 *
 * Uses ChatClient fluent API:
 *   chatClient.prompt().system(...).user(...).call().content();
 *
 * Document text is read via getText() (per Spring AI 1.0+ Content API).
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final DocumentRetrievalService retrievalService;
    private final CacheService cacheService;
    private final ChatHistoryService historyService;
    private final EventPublisher events;
    private final MeterRegistry metrics;
    private final ChatMetricsService chatMetrics;
    private final PreferenceService preferenceService;
    private final PromptCacheService promptCache;
    private final GuardrailsService guardrails;
    private final WebSearchService web;
    private final TenantSettingsService tenantSettings;
    private final QuotaService quotas;
    private final ModelRoutingService modelRouting;
    private final double scoreThreshold;
    private final boolean rerankEnabled;
    private final Reranker reranker;
    private final BudgetService budgets;
    private final double costPerCallUsd;
    private final ModelProviderRouter modelRouter;
    private final RemoteModelProxyClient modelProxyClient;

    public ChatService(ChatClient chatClient,
                       DocumentRetrievalService retrievalService,
                       CacheService cacheService,
                       ChatHistoryService historyService,
                       EventPublisher events,
                       MeterRegistry metrics,
                       ChatMetricsService chatMetrics,
                       PreferenceService preferenceService,
                       BudgetService budgets,
                       PromptCacheService promptCache,
                       GuardrailsService guardrails,
                       WebSearchService web,
                       TenantSettingsService tenantSettings,
                       QuotaService quotas,
                       ModelRoutingService modelRouting,
                       ModelProviderRouter modelRouter,
                       RemoteModelProxyClient modelProxyClient,
                       @Value("${owl.retrieval.score-threshold:0.45}") double scoreThreshold,
                       @Value("${owl.rerank.enabled:true}") boolean rerankEnabled,
                       @Value("${owl.cost.estimatePerCallUsd:0.0005}") double costPerCallUsd,
                       Reranker reranker) {
        this.chatClient = chatClient;
        this.retrievalService = retrievalService;
        this.cacheService = cacheService;
        this.historyService = historyService;
        this.events = events;
        this.metrics = metrics;
        this.chatMetrics = chatMetrics;
        this.preferenceService = preferenceService;
        this.budgets = budgets;
        this.promptCache = promptCache;
        this.guardrails = guardrails;
        this.web = web;
        this.tenantSettings = tenantSettings;
        this.quotas = quotas;
        this.modelRouting = modelRouting;
        this.scoreThreshold = scoreThreshold;
        this.rerankEnabled = rerankEnabled;
        this.reranker = reranker;
        this.costPerCallUsd = costPerCallUsd;
        this.modelRouter = modelRouter;
        this.modelProxyClient = modelProxyClient;
    }

    public ChatResponse answer(ChatRequest req) {
        final String tenantId = req.tenantId();
        final String query    = req.question();

        // -1) Quota check
        if (!quotas.allowRequest(tenantId)) {
            String msg = "Quota exceeded for this tenant. Please upgrade your plan or try later.";
            String chatId = historyService.save(tenantId, query, msg, false, List.of());
            return new ChatResponse(msg, List.of(), chatId, "REFUSE");
        }
        quotas.recordRequest(tenantId);

        // 0) Pre-safety check
        var questionSafety = guardrails.classifyQuestion(query);
        if (questionSafety == GuardrailsService.SafetyOutcome.REFUSE) {
            String msg = "I can’t assist with that request.";
            String chatId = historyService.save(tenantId, query, msg, false, List.of());
            return new ChatResponse(msg, List.of(), chatId, "REFUSE");
        }

        // 1) Semantic cache
        var cached = cacheService.lookup(tenantId, query);
        if (cached.isPresent()) {
            metrics.counter("chat.requests", "tenantId", tenantId, "cache", "hit").increment();
            String ans = cached.get();
            List<String> noSources = List.of();
            String chatId = historyService.save(tenantId, query, ans, true, noSources);
            events.chat(tenantId, query, true);
            chatMetrics.incHit(tenantId);
            return new ChatResponse(ans, noSources, chatId, "SAFE");
        }

        // 2a) Prompt cache (exact prompt/model caching)
        var sel = modelRouting.getForTenant(tenantId);
        String modelId = (sel.provider() == null ? "ollama" : sel.provider()) + ":" + (sel.chatModel() == null ? "default" : sel.chatModel());
        String cachedPrompt = promptCache.lookup(tenantId, modelId, query);
        ChatClient chatToUse = (modelRouter != null) ? modelRouter.chatClientFor(tenantId, sel) : chatClient;
        if (cachedPrompt != null) {
            String chatId = historyService.save(tenantId, query, cachedPrompt, true, List.of());
            return new ChatResponse(cachedPrompt, List.of(), chatId);
        }

        // 2b) Preference memory (highly-rated past answers)
        var preferred = preferenceService.lookup(tenantId, query);
        if (preferred.isPresent()) {
            String ans = preferred.get();
            List<String> noSources = List.of();
            String chatId = historyService.save(tenantId, query, ans, true, noSources);
            events.chat(tenantId, query, true);
            metrics.counter("chat.requests", "tenantId", tenantId, "cache", "pref").increment();
            return new ChatResponse(ans, noSources, chatId);
        }

        // 3) Retrieval (vector search; per-tenant)
        var hits = retrievalService.search(tenantId, query, req.document(), 8);
        if (rerankEnabled && reranker != null) {
            hits = reranker.rerank(tenantId, query, hits);
        }
        List<DocumentRetrievalService.Scored> strong = hits.stream()
                .filter(s -> s.score() >= scoreThreshold)
                .limit(5)
                .toList();

        // 4) Guardrails: grounded-only unless allowWeb == true
        if (strong.isEmpty() && !req.allowWeb()) {
            metrics.counter("chat.requests", "tenantId", tenantId, "cache", "miss", "answer", "empty").increment();
            String noAns = "I don't know based on the provided knowledge.";
            cacheService.save(tenantId, query, noAns);
            String chatId = historyService.save(tenantId, query, noAns, false, List.of());
            events.chat(tenantId, query, false);
            chatMetrics.incMissEmpty(tenantId);
            // consider fallback features
            boolean fallbackAllowed = req.fallback() != null && Boolean.TRUE.equals(req.fallback().enabled());
            fallbackAllowed = fallbackAllowed || tenantSettings.getOrCreate(tenantId).isFallbackEnabled();
            if (fallbackAllowed) {
                int maxCalls = req.fallback() != null && req.fallback().maxWebCalls() != null ? req.fallback().maxWebCalls() : 2;
                List<org.springframework.ai.document.Document> webDocs = web.search(query, Math.max(1, maxCalls));
                if (!webDocs.isEmpty()) {
                    StringBuilder webCtx = new StringBuilder();
                    for (var d : webDocs) webCtx.append(d.getText()).append("\n---\n");
                    String systemText = "Use ONLY the information in <CONTEXT> to answer. If unknown, say you don't know. <CONTEXT>\n" + webCtx + "\n</CONTEXT>";
                    if (!budgets.allowSpend(tenantId, costPerCallUsd)) {
                        String msg = "Budget exceeded for this tenant. Please try later.";
                        String id2 = historyService.save(tenantId, query, msg, false, List.of());
                        return new ChatResponse(msg, List.of(), id2, "REFUSE");
                    }
                    String modelAnswer = (modelProxyClient != null && modelProxyClient.isEnabled())
                            ? modelProxyClient.chat(tenantId, sel.provider(), sel.chatModel(), systemText, query)
                            : chatToUse.prompt().system(systemText).user(query).call().content();
                    // Post safety
                    var outSafety = guardrails.classifyAnswer(modelAnswer);
                    if (outSafety == GuardrailsService.SafetyOutcome.REFUSE) {
                        String msg = "I can't provide that information.";
                        String id2 = historyService.save(tenantId, query, msg, false, List.of());
                        return new ChatResponse(msg, List.of(), id2, "REFUSE");
                    }
                    promptCache.save(tenantId, modelId, query, modelAnswer);
                    String id2 = historyService.save(tenantId, query, modelAnswer, false, List.of());
                    return new ChatResponse(modelAnswer, List.of(), id2, "SAFE");
                }
            }
            return new ChatResponse(noAns, List.of(), chatId, "SAFE");
        }

        // 5) Build grounded context (Document#getText() in Spring AI 1.0+)
        StringBuilder ctx = new StringBuilder();
        for (var s : strong) {
            ctx.append(s.doc().getText()).append("\n---\n");
        }

        String systemText = """
            You are Owl, a helpful assistant for enterprise knowledge.
            Use ONLY the information inside <CONTEXT>. If the answer is not in <CONTEXT>,
            reply exactly: "I don't know based on the provided knowledge."
            Keep answers concise. If helpful, add a brief "Sources" list.
            <CONTEXT>
            """ + ctx + "\n</CONTEXT>";

        // 6) Cost guardrails (optional budget enforcement)
        if (!budgets.allowSpend(tenantId, costPerCallUsd)) {
            String msg = "Budget exceeded for this tenant. Please try later.";
            String chatId = historyService.save(tenantId, query, msg, false, List.of());
            return new ChatResponse(msg, List.of(), chatId);
        }

        // 7) Call the model via the fluent ChatClient API
        Timer.Sample sample = Timer.start();
        long t0 = System.currentTimeMillis();
        String modelAnswer = (modelProxyClient != null && modelProxyClient.isEnabled())
                ? modelProxyClient.chat(tenantId, sel.provider(), sel.chatModel(), systemText, query)
                : chatToUse.prompt().system(systemText).user(query).call().content();
        long dur = System.currentTimeMillis() - t0;
        sample.stop(metrics.timer("chat.model.time", "tenantId", tenantId));
        chatMetrics.addModelMs(tenantId, dur);
        budgets.recordSpend(tenantId, costPerCallUsd);
        metrics.counter("chat.cost.usd", "tenantId", tenantId).increment(costPerCallUsd);
        events.cost(tenantId, costPerCallUsd, dur);

        // 6) Append brief source hints
        List<String> sources = strong.stream().map(s -> {
            var md = s.doc().getMetadata();
            return Objects.toString(md.getOrDefault("filename", md.getOrDefault("url", "doc")));
        }).distinct().collect(Collectors.toList());

        StringBuilder sb = new StringBuilder(modelAnswer);
        if (!sources.isEmpty()) {
            sb.append("\n\nSources:\n");
            for (var name : sources) sb.append("- ").append(name).append("\n");
        }
        String finalAnswer = sb.toString();

        // Post-safety check
        var outSafety = guardrails.classifyAnswer(finalAnswer);
        if (outSafety == GuardrailsService.SafetyOutcome.REFUSE) {
            String msg = "I can't provide that information.";
            String id2 = historyService.save(tenantId, query, msg, false, List.of());
            return new ChatResponse(msg, List.of(), id2, "REFUSE");
        }

        // 7) Cache final answer
        cacheService.save(tenantId, query, finalAnswer);
        promptCache.save(tenantId, modelId, query, finalAnswer);
        String chatId = historyService.save(tenantId, query, finalAnswer, false, sources);
        events.chat(tenantId, query, false);
        metrics.counter("chat.requests", "tenantId", tenantId, "cache", "miss", "answer", "ok").increment();
        chatMetrics.incMissOk(tenantId);

        return new ChatResponse(finalAnswer, sources, chatId, "SAFE");
    }
}
