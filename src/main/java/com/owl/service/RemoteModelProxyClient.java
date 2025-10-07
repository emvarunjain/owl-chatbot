package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class RemoteModelProxyClient {
    private final WebClient http;
    private final boolean enabled;

    public RemoteModelProxyClient(@Value("${MODEL_PROXY_URL:}") String url) {
        this.enabled = url != null && !url.isBlank();
        this.http = enabled ? WebClient.builder().baseUrl(url).build() : null;
    }

    public boolean isEnabled() { return enabled; }

    public String chat(String tenantId, String provider, String model, String system, String user) {
        if (!enabled) throw new IllegalStateException("Model proxy not enabled");
        Map<String, Object> req = Map.of("tenantId", tenantId, "provider", provider, "model", model, "system", system, "user", user);
        Map<?, ?> res = http.post().uri("/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        Object answer = res.get("answer");
        return answer != null ? answer.toString() : "";
    }
}

