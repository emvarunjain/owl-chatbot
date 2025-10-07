package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class RemoteSafetyClient {
    private final WebClient http;
    private final boolean enabled;

    public RemoteSafetyClient(@Value("${owl.safety.remote.url:${OWL_SAFETY_REMOTE_URL:}}") String url) {
        this.enabled = url != null && !url.isBlank();
        this.http = enabled ? WebClient.builder().baseUrl(url).build() : null;
    }

    public boolean isEnabled() { return enabled; }

    public String classify(String text) {
        if (!enabled) throw new IllegalStateException("Remote safety not enabled");
        Map<?, ?> res = http.post().uri("/v1/safety/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        Object out = res.get("outcome");
        return out == null ? "REVIEW" : out.toString();
    }
}

