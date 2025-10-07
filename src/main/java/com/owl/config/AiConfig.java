package com.owl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Build a ChatClient from the auto-configured ChatClient.Builder.
 * Spring AI wires the underlying ChatModel (e.g., Ollama) from application.yml.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}