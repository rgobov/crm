package com.tryneuro.backend.controller.svelte;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryneuro.backend.service.EmbeddingService;
import com.tryneuro.backend.service.KnowledgeIngestService;
import com.tryneuro.backend.service.RagSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class AiInternalControllerRagTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RagSearchService ragSearchService;
    @MockBean private KnowledgeIngestService knowledgeIngestService;
    @MockBean private EmbeddingService embeddingService;

    private MockMvc mockMvc;
    private final String testSecret = "test-secret-key";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("POST /api/admin/ai/internal/knowledge/rag-search возвращает результаты")
    void ragSearchReturnsResults() throws Exception {
        var response = new com.tryneuro.backend.dto.ai.AiRagSearchResponse(List.of(
            new com.tryneuro.backend.dto.ai.AiRagSearchResponse.ChunkResult(
                "test content", 0.95, java.util.Map.of("source", "manual"))
        ));
        when(ragSearchService.search(anyString(), anyString(), anyInt())).thenReturn(response);

        String body = objectMapper.writeValueAsString(
            java.util.Map.of("query", "test", "tenantId", UUID.randomUUID().toString(), "topK", 5));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/rag-search")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chunks[0].content").value("test content"))
            .andExpect(jsonPath("$.chunks[0].score").value(0.95));
    }

    @Test
    @DisplayName("POST /knowledge/rag-search без query возвращает 400")
    void ragSearchWithoutQueryReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(
            java.util.Map.of("tenantId", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/rag-search")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /knowledge/rag-search c неверным secret возвращает 401")
    void ragSearchWithInvalidSecretReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(
            java.util.Map.of("query", "test", "tenantId", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/rag-search")
                .header("X-Internal-Secret", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /knowledge/ingest вызывает сервис")
    void ingestCallsService() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
            "tenantId", UUID.randomUUID().toString(),
            "knowledgeId", UUID.randomUUID().toString(),
            "text", "test content for ingestion"));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/ingest")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        verify(knowledgeIngestService).ingest(anyString(), anyString(), eq("test content for ingestion"));
    }

    @Test
    @DisplayName("POST /knowledge/ingest без text возвращает 400")
    void ingestWithoutTextReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
            "tenantId", UUID.randomUUID().toString(),
            "knowledgeId", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/ingest")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /knowledge/reindex вызывает сервис")
    void reindexCallsService() throws Exception {
        String body = objectMapper.writeValueAsString(
            java.util.Map.of("tenantId", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/admin/ai/internal/knowledge/reindex")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        verify(knowledgeIngestService).reindex(anyString());
    }
}
