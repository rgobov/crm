package com.tryneuro.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingService("", null);
    }

    @Test
    @DisplayName("Пустой api_key возвращает нулевой вектор размером 1536")
    void emptyApiKeyReturnsZeroVector() {
        List<Double> result = service.embed("test");
        assertEquals(1536, result.size());
        assertTrue(result.stream().allMatch(d -> d == 0.0));
    }

    @Test
    @DisplayName("Пустой текст не ломает сервис")
    void emptyTextReturnsZeroVector() {
        List<Double> result = service.embed("");
        assertEquals(1536, result.size());
        assertTrue(result.stream().allMatch(d -> d == 0.0));
    }
}
