package com.owl.service;

import com.owl.model.ModelCredentials;
import com.owl.service.ModelRoutingService.Selection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.api.AzureOpenAiApi;
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
        ModelCredentials mc = creds.get(tenantId, "openai");
        if (mc == null || mc.getApiKey() == null) return defaultClient;
        OpenAiApi api = new OpenAiApi(mc.getApiKey());
        ChatModel chatModel = new OpenAiChatModel(api, model);
        return ChatClient.builder(chatModel).build();
    }

    private ChatClient azureClient(String tenantId, String deployment) {
        ModelCredentials mc = creds.get(tenantId, "azure");
        if (mc == null || mc.getApiKey() == null || mc.getEndpoint() == null) return defaultClient;
        AzureOpenAiApi api = new AzureOpenAiApi(mc.getEndpoint(), mc.getApiKey());
        ChatModel chatModel = new AzureOpenAiChatModel(api, deployment);
        return ChatClient.builder(chatModel).build();
    }
}

