package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.ai.AiRagSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

class RagSearchServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EmbeddingService embeddingService;
    @InjectMocks private RagSearchService ragSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Нулевой эмбеддинг возвращает пустой результат")
    void zeroEmbeddingReturnsEmpty() {
        when(embeddingService.embed(anyString())).thenReturn(
            java.util.Collections.nCopies(1536, 0.0));

        AiRagSearchResponse result = ragSearchService.search(
            UUID.randomUUID().toString(), "test", 5);

        assertTrue(result.getChunks().isEmpty());
        verify(jdbcTemplate, never()).queryForList(anyString(), ArgumentMatchers.<Object[]>any());
    }

    @Test
    @DisplayName("Успешный поиск возвращает результаты с правильными полями")
    void successfulSearchReturnsResults() {
        String tenantId = UUID.randomUUID().toString();
        List<Double> shortEmbedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.embed(anyString())).thenReturn(shortEmbedding);

        List<Map<String, Object>> fakeRows = List.of(
            Map.of("content", "test content", "score", 0.95, "metadata", Map.of("source", "manual")),
            Map.of("content", "more content", "score", 0.85, "metadata", Map.of("source", "faq"))
        );
        doReturn(fakeRows).when(jdbcTemplate).queryForList(anyString(), ArgumentMatchers.any(Object[].class));

        AiRagSearchResponse result = ragSearchService.search(tenantId, "test", 5);

        assertEquals(2, result.getChunks().size());
        assertEquals("test content", result.getChunks().get(0).getContent());
        assertEquals(0.95, result.getChunks().get(0).getScore(), 0.001);
        assertEquals("manual", result.getChunks().get(0).getMetadata().get("source"));
    }

    @Test
    @DisplayName("Исключение в JdbcTemplate не прерывает выполнение, возвращается пустой список")
    void jdbcExceptionReturnsEmpty() {
        when(embeddingService.embed(anyString())).thenReturn(
            java.util.Collections.nCopies(1536, 0.1));
        when(jdbcTemplate.queryForList(anyString(), ArgumentMatchers.<Object[]>any()))
            .thenThrow(new RuntimeException("DB error"));

        AiRagSearchResponse result = ragSearchService.search(
            UUID.randomUUID().toString(), "test", 5);

        assertTrue(result.getChunks().isEmpty());
    }
}
