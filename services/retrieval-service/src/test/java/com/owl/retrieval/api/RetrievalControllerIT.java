package com.owl.retrieval.api;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RetrievalControllerIT {

    @Autowired MockMvc mvc;
    @MockBean VectorStore store;

    @org.springframework.test.context.DynamicPropertySource
    static void props(org.springframework.test.context.DynamicPropertyRegistry r) {
        r.add("spring.ai.vectorstore.qdrant.initialize-schema", () -> "false");
    }

    @Test
    void search_returns_docs_from_store() throws Exception {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("hello world", Map.of("tenantId","acme"))
        ));

        mvc.perform(post("/v1/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"acme\",\"q\":\"hello\",\"topK\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docs", hasSize(1)))
                .andExpect(jsonPath("$.docs[0].text", containsString("hello world")));

        ArgumentCaptor<SearchRequest> cap = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(cap.capture());
    }

    @Test
    void add_sends_docs_to_store() throws Exception {
        mvc.perform(post("/v1/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"acme\",\"docs\":[{\"text\":\"t\",\"metadata\":{}}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added", is(1)));
        verify(store).add(anyList());
    }
}
