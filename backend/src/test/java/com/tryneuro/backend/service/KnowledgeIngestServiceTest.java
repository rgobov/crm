package com.tryneuro.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeIngestServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EmbeddingService embeddingService;
    @InjectMocks private KnowledgeIngestService knowledgeIngestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Ingest разбивает текст на чанки и сохраняет каждый")
    void ingestSplitsTextAndInsertsChunks() {
        String tenantId = UUID.randomUUID().toString();
        String knowledgeId = UUID.randomUUID().toString();
        String text = "word " + "a ".repeat(1000);

        when(embeddingService.embed(anyString())).thenReturn(
            java.util.Collections.nCopies(1536, 0.5));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), anyInt(), anyString(), anyString()))
            .thenReturn(1);

        knowledgeIngestService.ingest(tenantId, knowledgeId, text);

        verify(jdbcTemplate, atLeastOnce()).update(
            startsWith("INSERT INTO ai_knowledge_chunks"),
            anyString(), eq(tenantId), eq(knowledgeId), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("Короткий текст создаёт ровно один чанк")
    void shortTextCreatesOneChunk() {
        String tenantId = UUID.randomUUID().toString();
        String knowledgeId = UUID.randomUUID().toString();
        String text = "short text for testing";

        when(embeddingService.embed(anyString())).thenReturn(
            java.util.Collections.nCopies(1536, 0.5));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), anyInt(), anyString(), anyString()))
            .thenReturn(1);

        knowledgeIngestService.ingest(tenantId, knowledgeId, text);

        verify(jdbcTemplate, times(1)).update(
            startsWith("INSERT INTO ai_knowledge_chunks"),
            anyString(), eq(tenantId), eq(knowledgeId), eq(0), anyString(), anyString());
    }

    @Test
    @DisplayName("Reindex удаляет старые чанки и пересоздаёт для всех знаний")
    void reindexClearsAndReingests() {
        String tenantId = UUID.randomUUID().toString();
        String kid1 = UUID.randomUUID().toString();
        String kid2 = UUID.randomUUID().toString();

        when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
            .thenReturn(List.of(kid1, kid2));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
            .thenReturn("question answer");
        when(embeddingService.embed(anyString())).thenReturn(
            java.util.Collections.nCopies(1536, 0.5));

        knowledgeIngestService.reindex(tenantId);

        verify(jdbcTemplate).update("DELETE FROM ai_knowledge_chunks WHERE tenant_id = ?::uuid", tenantId);
        verify(jdbcTemplate, times(1)).queryForList(anyString(), eq(String.class), any());
        verify(jdbcTemplate, times(2)).queryForObject(anyString(), eq(String.class), any());
    }
}
