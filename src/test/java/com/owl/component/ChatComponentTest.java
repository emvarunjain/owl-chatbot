package com.owl.component;

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
 * Component test for the complete chat flow with realistic data
 */
@ExtendWith(MockitoExtension.class)
class ChatComponentTest {

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
        // Setup ChatClient mock with realistic responses
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Based on the provided context, here is the answer...");

        // Setup realistic mocks
        when(quotaService.allowRequest(anyString())).thenReturn(true);
        when(guardrailsService.classifyQuestion(anyString())).thenReturn(GuardrailsService.SafetyOutcome.SAFE);
        when(guardrailsService.classifyAnswer(anyString())).thenReturn(GuardrailsService.SafetyOutcome.SAFE);
        when(budgetService.allowSpend(anyString(), anyDouble())).thenReturn(true);
        when(modelRoutingService.getForTenant(anyString())).thenReturn(new ModelRoutingService.Selection("ollama", "llama2", null));
        when(modelProviderRouter.chatClientFor(anyString(), any())).thenReturn(chatClient);
        when(historyService.save(anyString(), anyString(), anyString(), anyBoolean(), anyList())).thenReturn("chat-" + System.currentTimeMillis());

        chatService = new ChatService(
            chatClient, retrievalService, cacheService, historyService, eventPublisher,
            new SimpleMeterRegistry(), chatMetricsService, preferenceService, budgetService,
            promptCacheService, guardrailsService, webSearchService, tenantSettingsService,
            quotaService, modelRoutingService, modelProviderRouter, remoteModelProxyClient,
            0.45, true, 0.0005, reranker
        );
    }

    @Test
    void chat_withCompanyPolicyQuestion_shouldReturnGroundedResponse() {
        // Given - Realistic company policy scenario
        String tenantId = "acme-corp";
        String question = "What is our company policy on remote work?";
        
        // Mock realistic document retrieval results
        Document policyDoc = new Document(
            "Remote Work Policy: Employees are allowed to work remotely up to 3 days per week. " +
            "Remote work requests must be approved by direct managers. " +
            "Employees working remotely must maintain regular communication and attend all scheduled meetings.",
            Map.of(
                "filename", "hr-policy-2024.pdf",
                "score", 0.95,
                "tenantId", tenantId,
                "source", "hr-policy-2024.pdf"
            )
        );
        
        Document guidelinesDoc = new Document(
            "Remote Work Guidelines: Ensure you have a dedicated workspace. " +
            "Use company-approved communication tools. " +
            "Maintain regular working hours and be available during core business hours.",
            Map.of(
                "filename", "remote-work-guidelines.pdf",
                "score", 0.88,
                "tenantId", tenantId,
                "source", "remote-work-guidelines.pdf"
            )
        );
        
        List<DocumentRetrievalService.Scored> searchResults = List.of(
            new DocumentRetrievalService.Scored(policyDoc, 0.95),
            new DocumentRetrievalService.Scored(guidelinesDoc, 0.88)
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
        assertTrue(response.answer().contains("remote work"));
        assertTrue(response.answer().contains("3 days per week"));
        assertEquals(2, response.sources().size());
        assertTrue(response.sources().contains("hr-policy-2024.pdf"));
        assertTrue(response.sources().contains("remote-work-guidelines.pdf"));
        assertEquals("SAFE", response.safety());
        assertNotNull(response.chatId());

        // Verify interactions
        verify(retrievalService).search(tenantId, question, null, 10);
        verify(reranker).rerank(question, question, searchResults);
        verify(cacheService).save(eq(tenantId), eq(question), anyString());
        verify(chatClient).prompt();
        verify(historyService).save(eq(tenantId), eq(question), anyString(), eq(false), anyList());
    }

    @Test
    void chat_withTechnicalQuestion_shouldReturnDetailedResponse() {
        // Given - Technical documentation scenario
        String tenantId = "tech-corp";
        String question = "How do I configure the API authentication?";
        
        Document apiDoc = new Document(
            "API Authentication Configuration: Use OAuth 2.0 with client credentials flow. " +
            "Set the authorization header as 'Bearer {token}'. " +
            "Configure the client ID and secret in your application settings. " +
            "The token expires after 1 hour and must be refreshed.",
            Map.of(
                "filename", "api-documentation.pdf",
                "score", 0.92,
                "tenantId", tenantId,
                "source", "api-documentation.pdf"
            )
        );
        
        List<DocumentRetrievalService.Scored> searchResults = List.of(
            new DocumentRetrievalService.Scored(apiDoc, 0.92)
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
        assertTrue(response.answer().contains("OAuth 2.0"));
        assertTrue(response.answer().contains("Bearer"));
        assertTrue(response.answer().contains("client credentials"));
        assertEquals(1, response.sources().size());
        assertTrue(response.sources().contains("api-documentation.pdf"));
        assertEquals("SAFE", response.safety());

        verify(retrievalService).search(tenantId, question, null, 10);
        verify(chatClient).prompt();
    }

    @Test
    void chat_withNoRelevantDocuments_shouldReturnIDKResponse() {
        // Given - No relevant documents found
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
        assertEquals("SAFE", response.safety());

        verify(retrievalService).search(tenantId, question, null, 10);
        verify(cacheService).save(eq(tenantId), eq(question), contains("I don't know"));
        verify(chatClient, never()).prompt(); // Should not call AI model
    }

    @Test
    void chat_withWebFallback_shouldUseWebSearch() {
        // Given - Question requiring web search
        String tenantId = "research-org";
        String question = "What are the latest developments in quantum computing?";
        
        Document webDoc = new Document(
            "Latest Quantum Computing Developments: IBM announced new quantum processors with 1000+ qubits. " +
            "Google demonstrated quantum supremacy in error correction. " +
            "Microsoft released new quantum development tools.",
            Map.of(
                "url", "https://techcrunch.com/quantum-computing-2024",
                "score", 0.85,
                "tenantId", tenantId,
                "source", "https://techcrunch.com/quantum-computing-2024"
            )
        );
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.empty());
        when(retrievalService.search(tenantId, question, null, 10)).thenReturn(List.of());
        when(webSearchService.search(question, 2)).thenReturn(List.of(webDoc));
        // Mock response is already set up in setup()

        ChatRequest.FallbackPolicy fallbackPolicy = new ChatRequest.FallbackPolicy(true, 0.01, 2);
        ChatRequest request = new ChatRequest(tenantId, question, false, null, fallbackPolicy);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertTrue(response.answer().contains("quantum computing"));
        assertTrue(response.answer().contains("IBM"));
        assertTrue(response.answer().contains("Google"));
        assertEquals("SAFE", response.safety());

        verify(webSearchService).search(question, 2);
        verify(chatClient).prompt();
    }

    @Test
    void chat_withCachedResponse_shouldReturnCachedAnswer() {
        // Given - Cached response scenario
        String tenantId = "frequent-user";
        String question = "What is our refund policy?";
        String cachedAnswer = "Our refund policy allows returns within 30 days of purchase with original receipt.";
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.of(cachedAnswer));

        ChatRequest request = new ChatRequest(tenantId, question, false, null, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertEquals(cachedAnswer, response.answer());
        assertTrue(response.sources().isEmpty());
        assertEquals("SAFE", response.safety());

        verify(cacheService).lookup(tenantId, question);
        verify(historyService).save(tenantId, question, cachedAnswer, true, List.of());
        verify(chatClient, never()).prompt(); // Should not call AI model
    }

    @Test
    void chat_withUnsafeContent_shouldReturnRefusalMessage() {
        // Given - Unsafe content scenario
        String tenantId = "test-tenant";
        String question = "How to hack into computer systems?";
        
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
        verify(retrievalService, never()).search(anyString(), anyString(), any(), anyInt());
    }

    @Test
    void chat_withQuotaExceeded_shouldReturnQuotaMessage() {
        // Given - Quota exceeded scenario
        String tenantId = "over-limit-tenant";
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
    void chat_withDocumentScope_shouldSearchSpecificDocument() {
        // Given - Document-specific question
        String tenantId = "legal-firm";
        String question = "What are the key terms in the contract?";
        String documentScope = "contract-2024.pdf";
        
        Document contractDoc = new Document(
            "Contract Terms: The agreement is valid for 2 years. " +
            "Payment terms are net 30 days. " +
            "Termination requires 60 days notice.",
            Map.of(
                "filename", "contract-2024.pdf",
                "score", 0.98,
                "tenantId", tenantId,
                "source", "contract-2024.pdf"
            )
        );
        
        List<DocumentRetrievalService.Scored> searchResults = List.of(
            new DocumentRetrievalService.Scored(contractDoc, 0.98)
        );
        
        when(cacheService.lookup(tenantId, question)).thenReturn(Optional.empty());
        when(retrievalService.search(tenantId, question, documentScope, 10)).thenReturn(searchResults);
        when(reranker.rerank(question, question, searchResults)).thenReturn(searchResults);
        // Mock response is already set up in setup()

        ChatRequest request = new ChatRequest(tenantId, question, false, documentScope, null);

        // When
        ChatResponse response = chatService.answer(request);

        // Then
        assertNotNull(response);
        assertTrue(response.answer().contains("2 years"));
        assertTrue(response.answer().contains("net 30 days"));
        assertTrue(response.answer().contains("60 days notice"));
        assertEquals(1, response.sources().size());
        assertTrue(response.sources().contains("contract-2024.pdf"));

        verify(retrievalService).search(tenantId, question, documentScope, 10);
        verify(chatClient).prompt();
    }
}
