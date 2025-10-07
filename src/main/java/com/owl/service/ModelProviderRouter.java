package com.owl.service;

import com.owl.model.ModelCredentials;
import com.owl.service.ModelRoutingService.Selection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
// import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
// import org.springframework.ai.azure.openai.api.AzureOpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class ModelProviderRouter {
    private final ChatClient defaultClient;
    private final ModelCredentialsService creds;

    public ModelProviderRouter(ChatClient defaultClient, ModelCredentialsService creds) {
        this.defaultClient = defaultClient;
        this.creds = creds;
    }

    public ChatClient chatClientFor(String tenantId, Selection sel) {
        String provider = sel.provider() == null ? "ollama" : sel.provider();
        return switch (provider) {
            case "openai" -> openAiClient(tenantId, sel.chatModel());
            case "azure" -> azureClient(tenantId, sel.chatModel());
            case "bedrock" -> defaultClient; // TODO: implement bedrock router when keys are provided
            default -> defaultClient;
        };
    }

    private ChatClient openAiClient(String tenantId, String model) {
        // TODO: Fix OpenAI integration when proper API classes are available
        return defaultClient;
    }

    private ChatClient azureClient(String tenantId, String deployment) {
        // TODO: Fix Azure OpenAI integration when proper API classes are available
        return defaultClient;
    }
}

