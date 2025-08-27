package com.owl.controller;

import com.owl.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) { this.chat = chat; }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatService.ChatRequest req) {
        return ResponseEntity.ok(chat.answer(req));
    }
}
