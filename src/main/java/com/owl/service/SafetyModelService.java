package com.owl.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class SafetyModelService {
    private final ChatClient chat;
    private final PolicyLoader policies;
    private final boolean enabled;
    private final String model; // e.g., ollama:llama-guard
    private final String ollamaBase;
    private final RemoteSafetyClient remote;

    public SafetyModelService(ChatClient chat, PolicyLoader policies,
                              @Value("${owl.guardrails.enabled:false}") boolean enabled,
                              @Value("${owl.safety.model:}") String model,
                              @Value("${owl.safety.ollama.baseUrl:${SPRING_AI_OLLAMA_BASE_URL:http://localhost:11434}}") String ollamaBase,
                              RemoteSafetyClient remote) {
        this.chat = chat; this.policies = policies; this.enabled = enabled; this.model = model; this.ollamaBase = ollamaBase; this.remote = remote;
    }

    public String classify(String text) {
        if (!enabled) return "SAFE";
        if (remote != null && remote.isEnabled()) {
            try { return remote.classify(text); } catch (Exception ignored) {}
        }
        if (model != null && model.startsWith("ollama:")) {
            String m = model.substring("ollama:".length());
            return classifyWithOllama(m, text);
        }
        return classifyWithChatClient(text);
    }

    private String classifyWithChatClient(String text) {
        String system = """
            You are a strict safety classifier. Follow the policy strictly.
            POLICY:\n
            """ + policies.loadSafetyPolicy();
        String prompt = "Classify the following content as SAFE or REFUSE. Respond with exactly one token: SAFE or REFUSE.\n\n" + text;
        try {
            String out = chat.prompt().system(system).user(prompt).call().content();
            out = out == null ? "SAFE" : out.trim().toUpperCase();
            if (out.contains("REFUSE")) return "REFUSE";
            return "SAFE";
        } catch (Exception e) { return "REVIEW"; }
    }

    private String classifyWithOllama(String model, String text) {
        try {
            String policy = policies.loadSafetyPolicy();
            String prompt = "Classify as SAFE or REFUSE based on this policy:\n" + policy + "\n\nContent:\n" + text;
            String body = "{\"model\":\"" + escape(model) + "\",\"prompt\":\"" + escape(prompt) + "\",\"stream\":false}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(ollamaBase.replaceAll("/+$","") + "/api/generate"))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return "REVIEW";
            String out = resp.body();
            // Ollama returns JSON with field "response"
            int i = out.indexOf("\"response\":");
            if (i >= 0) {
                int s = out.indexOf('"', i + 11);
                int e = out.indexOf('"', s + 1);
                if (s >= 0 && e > s) {
                    String respText = out.substring(s + 1, e).trim().toUpperCase();
                    if (respText.contains("REFUSE")) return "REFUSE";
                }
            }
            return "SAFE";
        } catch (Exception e) { return "REVIEW"; }
    }

    private static String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n"); }
}
