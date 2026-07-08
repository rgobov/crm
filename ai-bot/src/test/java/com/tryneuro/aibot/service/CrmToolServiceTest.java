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

    @Test
    @DisplayName("getToolDefinitions возвращает 24 определения")
    void getToolDefinitionsReturns24Defs() {
        List<CrmToolService.ToolDef> defs = crmToolService.getToolDefinitions();
        assertEquals(24, defs.size());
    }

    @Test
    @DisplayName("Все ToolDef имеют name, description и parameters")
    void allToolDefsHaveRequiredFields() {
        List<CrmToolService.ToolDef> defs = crmToolService.getToolDefinitions();
        for (CrmToolService.ToolDef def : defs) {
            assertNotNull(def.name(), "name is required");
            assertNotNull(def.description(), "description is required");
            assertNotNull(def.parameters(), "parameters is required");
            assertEquals("object", def.parameters().get("type"));
            assertNotNull(def.parameters().get("properties"));
            assertNotNull(def.parameters().get("required"));
        }
    }

    @Test
    @DisplayName("ToolDef имена совпадают со схемами")
    void toolDefNamesMatchSchemas() {
        List<CrmToolService.ToolDef> defs = crmToolService.getToolDefinitions();
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        List<String> defNames = defs.stream().map(CrmToolService.ToolDef::name).toList();
        for (Map<String, Object> schema : schemas) {
            String name = (String) ((Map<String, Object>) schema.get("function")).get("name");
            assertTrue(defNames.contains(name), "ToolDef missing: " + name);
        }
    }
}
