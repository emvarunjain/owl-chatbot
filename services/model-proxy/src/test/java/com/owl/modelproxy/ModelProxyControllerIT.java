package com.owl.modelproxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ModelProxyControllerIT {

    static WireMockServer wm;

    @BeforeAll
    static void up() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        wm.stubFor(WireMock.post(WireMock.urlEqualTo("/v1/chat/completions"))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type","application/json")
                        .withBody("{\n  \"id\":\"cmpl\",\n  \"object\":\"chat.completion\",\n  \"choices\":[{\n    \"index\":0,\n    \"message\":{\"role\":\"assistant\",\"content\":\"Hi from fake OpenAI\"}\n  }]\n}")));
    }

    @AfterAll
    static void down() { wm.stop(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("OPENAI_API_KEY", () -> "test-key");
        r.add("OPENAI_BASE_URL", () -> "http://localhost:" + wm.port());
    }

    @Autowired MockMvc mvc;

    @Test
    void routes_to_openai_and_returns_answer() throws Exception {
        String payload = "{\"tenantId\":\"t\",\"provider\":\"openai\",\"model\":\"gpt-4o-mini\",\"system\":\"s\",\"user\":\"u\"}";
        mvc.perform(post("/v1/chat").contentType("application/json").content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hi from fake OpenAI"));
    }
}

