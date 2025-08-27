package com.owl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;

@Configuration
public class AiConfig {

    // Spring AI auto-configures an OllamaChatModel from your application.yml/.env
    // We just wrap it with the ergonomic ChatClient.
    @Bean
    public ChatClient chatClient(OllamaChatModel model) {
        return ChatClient.builder(model).build();
    }
}
