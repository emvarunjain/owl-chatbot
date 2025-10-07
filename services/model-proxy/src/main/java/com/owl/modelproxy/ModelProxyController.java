package com.owl.modelproxy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.api.AzureOpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class ModelProxyController {
    private final ChatClient defaultClient; // Ollama-based
    private final String openAiKey;
    private final String openAiBase;
    private final String azureKey;
    private final String azureEndpoint;

    public ModelProxyController(ChatClient chat,
                                @Value("${OPENAI_API_KEY:}") String openAiKey,
                                @Value("${OPENAI_BASE_URL:https://api.openai.com}") String openAiBase,
                                @Value("${AZURE_OPENAI_API_KEY:}") String azureKey,
                                @Value("${AZURE_OPENAI_ENDPOINT:}") String azureEndpoint) {
        this.defaultClient = chat; this.openAiKey=openAiKey; this.openAiBase=openAiBase; this.azureKey=azureKey; this.azureEndpoint=azureEndpoint;
    }

    public record ChatReq(String tenantId, String provider, String model, String system, String user) {}
    public record ChatRes(String answer) {}

    @PostMapping("/chat")
    public ResponseEntity<ChatRes> chat(@RequestBody ChatReq req) {
        ChatClient toUse = switch (safe(req.provider())) {
            case "openai" -> openAi(req.model());
            case "azure" -> azureOpenAi(req.model());
            case "bedrock" -> bedrock(req.model());
            default -> defaultClient;
        };
        String ans = toUse.prompt().system(req.system()).user(req.user()).call().content();
        return ResponseEntity.ok(new ChatRes(ans));
    }

    private ChatClient openAi(String model) {
        if (openAiKey == null || openAiKey.isBlank()) return defaultClient;
        OpenAiApi api = new OpenAiApi(openAiBase, openAiKey);
        ChatModel m = new OpenAiChatModel(api, model);
        return ChatClient.builder(m).build();
    }

    private ChatClient azureOpenAi(String deployment) {
        if (azureKey == null || azureKey.isBlank() || azureEndpoint == null || azureEndpoint.isBlank()) return defaultClient;
        AzureOpenAiApi api = new AzureOpenAiApi(azureEndpoint, azureKey);
        ChatModel m = new AzureOpenAiChatModel(api, deployment);
        return ChatClient.builder(m).build();
    }

    private ChatClient bedrock(String model) {
        try {
            var region = System.getenv().getOrDefault("BEDROCK_REGION", System.getenv().getOrDefault("AWS_REGION", "us-east-1"));
            var api = new org.springframework.ai.bedrock.aws.api.BedrockAwsApi(
                    software.amazon.awssdk.regions.Region.of(region),
                    software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create()
            );
            ChatModel m = new org.springframework.ai.bedrock.aws.BedrockAwsChatModel(api, model);
            return ChatClient.builder(m).build();
        } catch (Throwable t) {
            return defaultClient;
        }
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
