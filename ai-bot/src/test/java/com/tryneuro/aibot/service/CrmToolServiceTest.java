package com.tryneuro.aibot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrmToolServiceTest {

    @Mock private RestTemplate rest;
    private CrmToolService crmToolService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        crmToolService = new CrmToolService(rest, objectMapper);
    }

    @Test
    @DisplayName("getToolSchemas возвращает 24 схемы инструментов")
    void getToolSchemasReturns24Schemas() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        assertEquals(24, schemas.size());
    }

    @Test
    @DisplayName("Все схемы имеют правильную структуру type=function + name + parameters")
    void allSchemasHaveCorrectStructure() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        for (Map<String, Object> schema : schemas) {
            assertEquals("function", schema.get("type"));
            Map<String, Object> function = (Map<String, Object>) schema.get("function");
            assertNotNull(function.get("name"), "name is required");
            assertNotNull(function.get("description"), "description is required");
            assertNotNull(function.get("parameters"), "parameters is required");
        }
    }

    @Test
    @DisplayName("Схемы содержат search_knowledge_rag")
    void schemasContainSearchKnowledgeRag() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        boolean found = schemas.stream()
            .map(s -> (Map<String, Object>) s.get("function"))
            .anyMatch(f -> "search_knowledge_rag".equals(f.get("name")));
        assertTrue(found, "search_knowledge_rag should be present");
    }

    @Test
    @DisplayName("Все имена инструментов уникальны")
    void allToolNamesAreUnique() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        long distinctCount = schemas.stream()
            .map(s -> (Map<String, Object>) s.get("function"))
            .map(f -> (String) f.get("name"))
            .distinct()
            .count();
        assertEquals(24, distinctCount);
    }
}
