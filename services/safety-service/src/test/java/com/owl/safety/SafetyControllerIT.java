package com.owl.safety;

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
class SafetyControllerIT {

    static WireMockServer wm;
    @BeforeAll static void up(){ wm=new WireMockServer(WireMockConfiguration.options().dynamicPort()); wm.start();
        wm.stubFor(WireMock.post(WireMock.urlEqualTo("/api/generate"))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type","application/json")
                        .withBody("{\"response\":\"SAFE\"}")));
    }
    @AfterAll static void down(){ wm.stop(); }

    @DynamicPropertySource static void props(DynamicPropertyRegistry r){
        r.add("OWL_SAFETY_MODEL", () -> "ollama:guard");
        r.add("SPRING_AI_OLLAMA_BASE_URL", () -> "http://localhost:"+wm.port());
    }

    @Autowired MockMvc mvc;

    @Test
    void classify_calls_ollama_and_returns_outcome() throws Exception {
        mvc.perform(post("/v1/safety/classify").contentType("application/json").content("{\"text\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SAFE"));
    }
}

