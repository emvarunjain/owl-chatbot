package com.owl.controller;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import com.owl.service.ChatService;
import com.owl.security.TenantAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;
    
    @Mock
    private TenantAuth tenantAuth;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, tenantAuth)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void chat_shouldReturnChatResponse_whenValidRequest() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("test-tenant", "What is the weather?", false, null, null);
        ChatResponse response = new ChatResponse("The weather is sunny today.", List.of(), "chat-123", "SAFE");
        
        when(chatService.answer(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The weather is sunny today."))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.chatId").value("chat-123"))
                .andExpect(jsonPath("$.safety").value("SAFE"));

        verify(tenantAuth).authorize("test-tenant");
        verify(chatService).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldReturnBadRequest_whenInvalidRequest() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("", "What is the weather?", false, null, null);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tenantAuth, never()).authorize(anyString());
        verify(chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldReturnBadRequest_whenMissingQuestion() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("test-tenant", "", false, null, null);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tenantAuth, never()).authorize(anyString());
        verify(chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldReturnBadRequest_whenInvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        verify(tenantAuth, never()).authorize(anyString());
        verify(chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldReturnBadRequest_whenMissingContentType() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("test-tenant", "What is the weather?", false, null, null);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(tenantAuth, never()).authorize(anyString());
        verify(chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldHandleFallbackPolicy() throws Exception {
        // Given
        ChatRequest.FallbackPolicy fallbackPolicy = new ChatRequest.FallbackPolicy(true, 0.01, 3);
        ChatRequest request = new ChatRequest("test-tenant", "What are the latest AI developments?", true, null, fallbackPolicy);
        ChatResponse response = new ChatResponse("Based on recent developments, AI technology is advancing rapidly.", List.of(), "chat-456", "SAFE");
        
        when(chatService.answer(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Based on recent developments, AI technology is advancing rapidly."))
                .andExpect(jsonPath("$.chatId").value("chat-456"));

        verify(tenantAuth).authorize("test-tenant");
        verify(chatService).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldHandleDocumentScope() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("test-tenant", "What is mentioned in the policy document?", false, "policy.pdf", null);
        ChatResponse response = new ChatResponse("The policy document mentions remote work guidelines.", List.of("policy.pdf"), "chat-789", "SAFE");
        
        when(chatService.answer(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The policy document mentions remote work guidelines."))
                .andExpect(jsonPath("$.sources[0]").value("policy.pdf"));

        verify(tenantAuth).authorize("test-tenant");
        verify(chatService).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldHandleServiceException() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("test-tenant", "What is the weather?", false, null, null);
        
        when(chatService.answer(any(ChatRequest.class)))
            .thenThrow(new RuntimeException("Service temporarily unavailable"));

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(tenantAuth).authorize("test-tenant");
        verify(chatService).answer(any(ChatRequest.class));
    }

    @Test
    void chat_shouldHandleUnauthorizedTenant() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("unauthorized-tenant", "What is the weather?", false, null, null);
        
        doThrow(new SecurityException("Unauthorized tenant"))
            .when(tenantAuth).authorize("unauthorized-tenant");

        // When & Then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(tenantAuth).authorize("unauthorized-tenant");
        verify(chatService, never()).answer(any(ChatRequest.class));
    }
}
