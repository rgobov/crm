package com.tryneuro.aibot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    @DisplayName("getToolSchemas возвращает 25 схем инструментов")
    void getToolSchemasReturns25Schemas() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        assertEquals(25, schemas.size());
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
        assertEquals(25, distinctCount);
    }

    @Test
    @DisplayName("getToolDefinitions возвращает 25 определений")
    void getToolDefinitionsReturns25Defs() {
        List<CrmToolService.ToolDef> defs = crmToolService.getToolDefinitions();
        assertEquals(25, defs.size());
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

    @Test
    @DisplayName("get_branches имеет опциональный параметр query")
    void toolSchemaGetBranchesHasQueryParam() {
        Map<String, Object> params = crmToolService.getToolDefinitions().stream()
            .filter(d -> "get_branches".equals(d.name()))
            .findFirst()
            .orElseThrow()
            .parameters();
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertNotNull(props.get("query"));
    }

    @Test
    @DisplayName("search_staff имеет параметр branch_id")
    void toolSchemaSearchStaffHasBranchIdParam() {
        Map<String, Object> params = crmToolService.getToolDefinitions().stream()
            .filter(d -> "search_staff".equals(d.name()))
            .findFirst()
            .orElseThrow()
            .parameters();
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertNotNull(props.get("branch_id"));
    }

    @Test
    @DisplayName("create_appointment имеет параметр staffId")
    void toolSchemaCreateAppointmentHasStaffIdParam() {
        Map<String, Object> params = crmToolService.getToolDefinitions().stream()
            .filter(d -> "create_appointment".equals(d.name()))
            .findFirst()
            .orElseThrow()
            .parameters();
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertNotNull(props.get("staffId"));
    }

    @Test
    @DisplayName("check_availability required включает staff_id, date, time, duration")
    void toolSchemaCheckAvailabilityRequiredIncludesTimeAndDuration() {
        Map<String, Object> params = crmToolService.getToolDefinitions().stream()
            .filter(d -> "check_availability".equals(d.name()))
            .findFirst()
            .orElseThrow()
            .parameters();
        List<String> required = (List<String>) params.get("required");
        assertTrue(required.contains("staff_id"));
        assertTrue(required.contains("date"));
        assertTrue(required.contains("time"));
        assertTrue(required.contains("duration"));
    }

    @Test
    @DisplayName("get_available_slots присутствует в схемах")
    void toolSchemaGetAvailableSlotsExists() {
        boolean found = crmToolService.getToolSchemas().stream()
            .map(s -> (Map<String, Object>) s.get("function"))
            .anyMatch(f -> "get_available_slots".equals(f.get("name")));
        assertTrue(found, "get_available_slots tool should be present");
    }

    @Test
    @DisplayName("get_available_slots required: staff_id, date")
    void toolSchemaGetAvailableSlotsRequired() {
        Map<String, Object> params = crmToolService.getToolDefinitions().stream()
            .filter(d -> "get_available_slots".equals(d.name()))
            .findFirst()
            .orElseThrow()
            .parameters();
        List<String> required = (List<String>) params.get("required");
        assertTrue(required.contains("staff_id"));
        assertTrue(required.contains("date"));
    }

    @Test
    @DisplayName("executeTool get_branches передаёт query параметром URL")
    void getBranchesAcceptsQueryAndReturnsIds() {
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new org.springframework.http.ResponseEntity<>("[{\"id\":\"b1\"}]", org.springframework.http.HttpStatus.OK));

        Map<String, Object> args = Map.of("query", "виртуальный");
        String result = crmToolService.executeTool("get_branches", args, "t1", Map.of());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).exchange(urlCap.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        assertTrue(urlCap.getValue().contains("query="));
        assertNotNull(result);
    }

    @Test
    @DisplayName("executeTool search_staff передаёт branchId в body")
    void searchStaffAcceptsBranchIdFilter() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("[]");

        Map<String, Object> args = Map.of("query", "", "branch_id", "b1");
        crmToolService.executeTool("search_staff", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(anyString(), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"branchId\":\"b1\""));
    }

    @Test
    @DisplayName("executeTool get_available_slots вызывает POST /availability/slots")
    void getAvailableSlotsPostsToSlotsEndpoint() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{\"slots\":[]}");

        Map<String, Object> args = Map.of("staff_id", "s1", "date", "2026-07-10", "duration", 60);
        crmToolService.executeTool("get_available_slots", args, "t1", Map.of());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).postForObject(urlCap.capture(), any(HttpEntity.class), eq(String.class));
        assertTrue(urlCap.getValue().endsWith("/availability/slots"));
    }

    @Test
    @DisplayName("executeTool create_appointment передаёт staffId из staffId (camelCase)")
    void createAppointmentSendsStaffIdWhenProvidedCamelCase() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{}");

        Map<String, Object> args = Map.of(
            "clientName", "Иван", "serviceName", "Стрижка", "dateTime", "2026-07-10T14:00:00+03:00",
            "staffId", "s1", "branchId", "b1");
        crmToolService.executeTool("create_appointment", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(anyString(), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"staffId\":\"s1\""));
    }

    @Test
    @DisplayName("executeTool update_appointment принимает snake_case (duration_minutes, date_time, service_name, staff_name)")
    void updateAppointmentUsesSnakeCaseArgs() {
        when(rest.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new org.springframework.http.ResponseEntity<>("{}", org.springframework.http.HttpStatus.OK));

        Map<String, Object> args = Map.of(
            "appointment_id", "a1", "duration_minutes", 30, "date_time", "2026-07-10T14:00:00+03:00",
            "service_name", "Стрижка", "staff_name", "Маша");
        crmToolService.executeTool("update_appointment", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).exchange(anyString(), eq(HttpMethod.PUT), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"durationMinutes\":30"));
        assertTrue(body.contains("\"dateTime\":\"2026-07-10T14:00:00+03:00\""));
        assertTrue(body.contains("\"serviceName\":\"Стрижка\""));
        assertTrue(body.contains("\"staffName\":\"Маша\""));
    }

    @Test
    @DisplayName("executeTool отклоняет имя как *_id (get_contact с contact_id=имя не дёргает RestTemplate)")
    void executeToolRejectsNameAsId() {
        Map<String, Object> args = Map.of("contact_id", "филиал виртуальный");

        String result = crmToolService.executeTool("get_contact", args, "t1", Map.of());

        verifyNoInteractions(rest);
        assertNotNull(result);
        assertTrue(result.contains("error"));
    }
}