package com.owl.safety;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/safety")
public class SafetyController {
    private final String ollamaBase;
    private final String model;
    private final String policy;

    public SafetyController(@Value("${SPRING_AI_OLLAMA_BASE_URL:http://localhost:11434}") String base,
                            @Value("${OWL_SAFETY_MODEL:}") String model) throws Exception {
        this.ollamaBase = base;
        this.model = model == null ? "" : model;
        this.policy = "Refuse requests for illegal/harmful content. Otherwise SAFE.";
    }

    public record Req(String text) {}
    public record Res(String outcome) {}

    @PostMapping("/classify")
    public ResponseEntity<Res> classify(@RequestBody Req req) throws Exception {
        String text = req.text() == null ? "" : req.text();
        if (!model.startsWith("ollama:")) {
            // Simple heuristic fallback
            String t = text.toLowerCase();
            if (t.contains("weapon") || t.contains("explosive") || t.contains("ssn")) return ResponseEntity.ok(new Res("REFUSE"));
            return ResponseEntity.ok(new Res("SAFE"));
        }
        String m = model.substring("ollama:".length());
        String prompt = "Classify SAFE or REFUSE based on policy: " + policy + "\nContent:\n" + text;
        String body = "{\"model\":\"" + escape(m) + "\",\"prompt\":\"" + escape(prompt) + "\",\"stream\":false}";
        HttpRequest httpReq = HttpRequest.newBuilder(URI.create(ollamaBase.replaceAll("/+$","") + "/api/generate"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HttpClient.newHttpClient().send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) return ResponseEntity.ok(new Res("REVIEW"));
        String out = resp.body().toUpperCase();
        return ResponseEntity.ok(new Res(out.contains("REFUSE") ? "REFUSE" : "SAFE"));
    }

    private static String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n"); }
}

