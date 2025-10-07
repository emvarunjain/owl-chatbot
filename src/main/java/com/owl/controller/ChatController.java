package com.owl.controller;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import com.owl.service.ChatService;
import com.owl.security.TenantAuth;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/chat", "/api/v1/chat"})
public class ChatController {

    private final ChatService chatService;
    private final TenantAuth tenantAuth;

    public ChatController(ChatService chatService, TenantAuth tenantAuth) {
        this.chatService = chatService;
        this.tenantAuth = tenantAuth;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        tenantAuth.authorize(request.tenantId());
        return ResponseEntity.ok(chatService.answer(request));
    }
}
