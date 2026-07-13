package com.tryneuro.aibot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserConfigServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private UserConfigService userConfigService;

    @Test
    @DisplayName("getConfig читает llm_provider из БД")
    void getConfigReturnsLlmProvider() {
        when(jdbcTemplate.queryForMap(anyString(), eq(123L)))
                .thenReturn(Map.of(
                        "api_key", "test-key",
                        "llm_model", "GigaChat",
                        "llm_provider", "local"
                ));

        UserConfigService.UserConfig cfg = userConfigService.getConfig(123L);
        assertNotNull(cfg);
        assertEquals("test-key", cfg.apiKey());
        assertEquals("GigaChat", cfg.llmModel());
        assertEquals("local", cfg.llmProvider());
    }

    @Test
    @DisplayName("getConfig возвращает дефолтный gigachat при null llm_provider в БД")
    void getConfigDefaultsToGigachatWhenNull() {
        Map<String, Object> row = new HashMap<>();
        row.put("api_key", "test-key-2");
        row.put("llm_model", "GigaChat-Pro");
        row.put("llm_provider", null);

        when(jdbcTemplate.queryForMap(anyString(), eq(456L)))
                .thenReturn(row);

        UserConfigService.UserConfig cfg = userConfigService.getConfig(456L);
        assertNotNull(cfg);
        assertEquals("gigachat", cfg.llmProvider());
    }

    @Test
    @DisplayName("getConfig возвращает null при ошибке запроса")
    void getConfigReturnsNullOnError() {
        when(jdbcTemplate.queryForMap(anyString(), eq(789L)))
                .thenThrow(new RuntimeException("DB error"));

        UserConfigService.UserConfig cfg = userConfigService.getConfig(789L);
        assertNull(cfg);
    }
}
