package com.owl.integration;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import com.owl.service.*;
import com.owl.rerank.Reranker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete chat flow with mock data
 */
@ExtendWith(MockitoExtension.class)
class ChatIntegrationTest {

    @Mock
    private ChatClient chatClient;
    
    @Mock
    private DocumentRetrievalService retrievalService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private ChatHistoryService historyService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private ChatMetricsService chatMetricsService;
    
    @Mock
    private PreferenceService preferenceService;
    
    @Mock
    private BudgetService budgetService;
    
    @Mock
    private PromptCacheService promptCacheService;
    
    @Mock
    private GuardrailsService guardrailsService;
    
    @Mock
    private WebSearchService webSearchService;
    
    @Mock
    private TenantSettingsService tenantSettingsService;
    
    @Mock
    private QuotaService quotaService;
    
    @Mock
    private ModelRoutingService modelRoutingService;
    
    @Mock
    private ModelProviderRouter modelProviderRouter;
    
    @Mock
    private RemoteModelProxyClient remoteModelProxyClient;
    
    @Mock
    private Reranker reranker;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // Setup ChatClient mock
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Mocked AI response");

        // Setup other mocks
        when(quotaService.allowRequest(anyString())).thenReturn(true);
        when(guardrailsService.classifyQuestion(anyString())).thenReturn(GuardrailsService.SafetyOutcome.SAFE);
        when(guardrailsService.classifyAnswer(anyString())).thenReturn(GuardrailsService.SafetyOutcome.SAFE);
        when(budgetService.allowSpend(anyString(), anyDouble())).thenReturn(true);
        when(modelRoutingService.getForTenant(anyString())).thenReturn(new ModelRoutingService.Selection("ollama", null, null));
        when(modelProviderRouter.chatClientFor(anyString(), any())).thenReturn(chatClient);
        when(historyService.save(anyString(), anyString(), anyString(), anyBoolean(), anyList())).thenReturn("chat-123");

        chatService = new ChatService(
            chatClient, retrievalService, cacheService, historyService, eventPublisher,
            new SimpleMeterRegistry(), chatMetricsService, preferenceService, budgetService,
            promptCacheService, guardrailsService, webSearchService, tenantSettingsService,
            quotaService, modelRoutingService, modelProviderRouter, remoteModelProxyClient,
            0.45, true, 0.0005, reranker
        );
    }

    @Test
    void chat_withCacheHit_shouldReturnCachedResponse() {
        // Given
        String tenantId = "test-tenant";
        String question = "What is the capital of France?";
        String cachedAnswer = "The capital of France is Paris.";
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.of(cachedAnswer));

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertEquals(cachedAnswer, response.answer());
        assertTrue(response.sources().isEmpty());
        assertEquals("chat-123", response.chatId());
        assertEquals("SAFE", response.safety());

        verify(cacheService).lookup(tenantId, question);
        verify(historyService).save(tenantId, question, cachedAnswer, true, List.of());
        verify(eventPublisher).chat(tenantId, question, true);
        verify(chatClient, never()).prompt(); // Should not call AI model
    }

    @Test
    void chat_withKnowledgeBaseHit_shouldReturnGroundedResponse() {
        // Given
        String tenantId = "test-tenant";
        String question = "What is our company policy on remote work?";
        String aiResponse = "Based on our company policy, remote work is allowed up to 3 days per week.";
        
        // Mock retrieval results
        Document doc1 = new Document("Remote work policy: Employees can work remotely up to 3 days per week.", 
            Map.of("filename", "hr-policy.pdf", "score", 0.95));
        Document doc2 = new Document("Work from home guidelines and procedures.", 
            Map.of("filename", "wfh-guidelines.pdf", "score", 0.88));
        
        List<DocumentRetrievalService.Scored> searchResults = List.of(
            new DocumentRetrievalService.Scored(doc1, 0.95),
            new DocumentRetrievalService.Scored(doc2, 0.88)
        );
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.empty());
        when(retrievalService.search(tenantId, question, null, 10)).thenReturn(searchResults);
        when(reranker.rerank(question, question, searchResults)).thenReturn(searchResults);
        // Mock response is already set up in setup()

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertEquals(aiResponse, response.answer());
        assertEquals(2, response.sources().size());
        assertTrue(response.sources().contains("hr-policy.pdf"));
        assertTrue(response.sources().contains("wfh-guidelines.pdf"));
        assertEquals("chat-123", response.chatId());
        assertEquals("SAFE", response.safety());

        verify(retrievalService).search(tenantId, question, null, 10);
        verify(reranker).rerank(question, question, searchResults);
        verify(cacheService).save(tenantId, question, aiResponse);
        verify(chatClient).prompt();
    }

    @Test
    void chat_withNoKnowledgeBaseHit_shouldReturnIDKResponse() {
        // Given
        String tenantId = "test-tenant";
        String question = "What is the meaning of life?";
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.empty());
        when(retrievalService.search(tenantId, question, null, 10)).thenReturn(List.of());

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertTrue(response.answer().toLowerCase().contains("i don't know"));
        assertTrue(response.sources().isEmpty());
        assertEquals("chat-123", response.chatId());
        assertEquals("SAFE", response.safety());

        verify(retrievalService).search(tenantId, question, null, 10);
        verify(cacheService).save(eq(tenantId), eq(question), contains("I don't know"));
        verify(chatClient, never()).prompt(); // Should not call AI model
    }

    @Test
    void chat_withWebFallback_shouldUseWebSearch() {
        // Given
        String tenantId = "test-tenant";
        String question = "What is the latest news about AI?";
        String webResponse = "Based on recent news, AI technology is advancing rapidly with new breakthroughs in language models.";
        
        // Mock web search results
        Document webDoc = new Document("Latest AI news: New language models show significant improvements in reasoning capabilities.", 
            Map.of("url", "https://example.com/ai-news"));
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.empty());
        when(retrievalService.search(tenantId, question, null, 10)).thenReturn(List.of());
        when(webSearchService.search(question, 2)).thenReturn(List.of(webDoc));
        // Mock response is already set up in setup()

        ChatRequest.FallbackPolicy fallbackPolicy = new ChatRequest.FallbackPolicy(true, null, 2);
        ChatRequest request = new ChatRequest(tenantId, question, false, null, fallbackPolicy);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertEquals(webResponse, response.answer());
        assertTrue(response.sources().isEmpty());
        assertEquals("chat-123", response.chatId());
        assertEquals("SAFE", response.safety());

        verify(webSearchService).search(question, 2);
        verify(chatClient).prompt();
    }

    @Test
    void chat_withQuotaExceeded_shouldReturnQuotaMessage() {
        // Given
        String tenantId = "test-tenant";
        String question = "What is the weather today?";
        
        when(quotaService.allowRequest(tenantId)).thenReturn(false);

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertTrue(response.answer().contains("Quota exceeded"));
        assertEquals("REFUSE", response.safety());

        verify(quotaService).allowRequest(tenantId);
        verify(quotaService, never()).recordRequest(anyString());
        verify(chatClient, never()).prompt();
    }

    @Test
    void chat_withUnsafeContent_shouldReturnRefusalMessage() {
        // Given
        String tenantId = "test-tenant";
        String question = "How to hack into a computer system?";
        
        when(guardrailsService.classifyQuestion(question)).thenReturn(GuardrailsService.SafetyOutcome.REFUSE);

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertTrue(response.answer().contains("I can't assist with that request"));
        assertEquals("REFUSE", response.safety());

        verify(guardrailsService).classifyQuestion(question);
        verify(chatClient, never()).prompt();
    }
}
