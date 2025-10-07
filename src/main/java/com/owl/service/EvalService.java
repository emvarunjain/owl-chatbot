package com.owl.service;

import com.owl.model.ChatRequest;
import com.owl.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvalService {
    private final ChatService chat;

    public static record Golden(String question, String mustContain) {}
    public static record Result(int total, int passed, List<String> failures) {}

    public EvalService(ChatService chat) { this.chat = chat; }

    public Result run(String tenantId, List<Golden> goldens) {
        int total = 0, passed = 0; List<String> failures = new ArrayList<>();
        for (Golden g : goldens) {
            total++;
            ChatRequest req = new ChatRequest(tenantId, g.question(), false, null, null);
            ChatResponse r = chat.answer(req);
            if (r.answer() != null && r.answer().toLowerCase().contains(g.mustContain().toLowerCase())) passed++;
            else failures.add(g.question());
        }
        return new Result(total, passed, failures);
    }
}

