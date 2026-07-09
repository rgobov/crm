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
    @DisplayName("getToolSchemas возвращает 27 схем инструментов")
    void getToolSchemasReturns27Schemas() {
        List<Map<String, Object>> schemas = crmToolService.getToolSchemas();
        assertEquals(27, schemas.size());
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
        assertEquals(27, distinctCount);
    }

    @Test
    @DisplayName("getToolDefinitions возвращает 27 определений")
    void getToolDefinitionsReturns27Defs() {
        List<CrmToolService.ToolDef> defs = crmToolService.getToolDefinitions();
        assertEquals(27, defs.size());
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

        Map<String, Object> args = Map.of("query", "", "branch_id", "b1b2c3d4-e5f6-7890-abcd-ef1234567891");
        crmToolService.executeTool("search_staff", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(anyString(), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"branchId\":\"b1b2c3d4-e5f6-7890-abcd-ef1234567891\""));
    }

    @Test
    @DisplayName("executeTool get_available_slots вызывает POST /availability/slots")
    void getAvailableSlotsPostsToSlotsEndpoint() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{\"slots\":[]}");

        Map<String, Object> args = Map.of("staff_id", "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "date", "2026-07-10", "duration", 60);
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
            "staffId", "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "branchId", "b1b2c3d4-e5f6-7890-abcd-ef1234567891");
        crmToolService.executeTool("create_appointment", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(anyString(), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"staffId\":\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\""));
    }

    @Test
    @DisplayName("executeTool update_appointment принимает snake_case (duration_minutes, date_time, service_name, staff_name)")
    void updateAppointmentUsesSnakeCaseArgs() {
        when(rest.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new org.springframework.http.ResponseEntity<>("{}", org.springframework.http.HttpStatus.OK));

        Map<String, Object> args = Map.of(
            "appointment_id", "c1b2c3d4-e5f6-7890-abcd-ef1234567892", "duration_minutes", 30, "date_time", "2026-07-10T14:00:00+03:00",
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

    @Test
    @DisplayName("get_branch_staff_slots присутствует в схемах")
    void toolSchemaGetBranchStaffSlotsExists() {
        boolean found = crmToolService.getToolSchemas().stream()
            .map(s -> (Map<String, Object>) s.get("function"))
            .anyMatch(f -> "get_branch_staff_slots".equals(f.get("name")));
        assertTrue(found, "get_branch_staff_slots tool should be present");
    }

    @Test
    @DisplayName("executeTool get_branch_staff_slots вызывает POST /availability/branch-slots")
    void getBranchStaffSlotsPostsToBranchSlotsEndpoint() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{\"staff\":[]}");

        Map<String, Object> args = Map.of("branch_id", "b1b2c3d4-e5f6-7890-abcd-ef1234567891", "date", "tomorrow", "duration", 60);
        crmToolService.executeTool("get_branch_staff_slots", args, "t1", Map.of());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).postForObject(urlCap.capture(), any(HttpEntity.class), eq(String.class));
        assertTrue(urlCap.getValue().endsWith("/availability/branch-slots"));
    }

    @Test
    @DisplayName("executeTool get_branch_staff_slots передаёт branchId/date/duration в body")
    void getBranchStaffSlotsSendsBody() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{}");

        Map<String, Object> args = Map.of("branch_id", "b1b2c3d4-e5f6-7890-abcd-ef1234567891", "date", "tomorrow");
        crmToolService.executeTool("get_branch_staff_slots", args, "t1", Map.of());

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(anyString(), entityCap.capture(), eq(String.class));
        String body = entityCap.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"branchId\":\"b1b2c3d4-e5f6-7890-abcd-ef1234567891\""));
        assertTrue(body.contains("\"date\":\"tomorrow\""));
        assertTrue(body.contains("\"duration\":60"));
    }

    @Test
    @DisplayName("toolsForRole(CLIENT) не содержит create_contact и содержит manage_notifications")
    void toolsForRoleClientExcludesContactManagement() {
        java.util.Set<String> clientTools = crmToolService.toolsForRole("CLIENT");
        assertFalse(clientTools.contains("create_contact"));
        assertFalse(clientTools.contains("update_contact"));
        assertFalse(clientTools.contains("delete_contact"));
        assertFalse(clientTools.contains("add_service"));
        assertFalse(clientTools.contains("delete_service"));
        assertFalse(clientTools.contains("get_report"));
        assertTrue(clientTools.contains("manage_notifications"));
        assertTrue(clientTools.contains("create_appointment"));
        assertTrue(clientTools.contains("get_branch_staff_slots"));
    }

    @Test
    @DisplayName("toolsForRole(EMPLOYEE) не содержит add_service/get_report но содержит create_appointment")
    void toolsForRoleEmployeeExcludesServiceManagement() {
        java.util.Set<String> empTools = crmToolService.toolsForRole("EMPLOYEE");
        assertFalse(empTools.contains("add_service"));
        assertFalse(empTools.contains("update_service"));
        assertFalse(empTools.contains("delete_service"));
        assertFalse(empTools.contains("get_report"));
        assertFalse(empTools.contains("create_contact"));
        assertTrue(empTools.contains("create_appointment"));
        assertTrue(empTools.contains("get_contact"));
        assertTrue(empTools.contains("get_branch_staff_slots"));
    }

    @Test
    @DisplayName("toolsForRole(ADMIN) и (MANAGER) содержат 26 tools (все кроме manage_notifications) включая create_contact, get_report и resolve_branch")
    void toolsForRoleAdminManagerAllTools() {
        java.util.Set<String> adminTools = crmToolService.toolsForRole("ADMIN");
        java.util.Set<String> managerTools = crmToolService.toolsForRole("MANAGER");
        assertEquals(26, adminTools.size());
        assertEquals(26, managerTools.size());
        assertTrue(adminTools.contains("create_contact"));
        assertTrue(adminTools.contains("get_report"));
        assertTrue(adminTools.contains("get_branch_staff_slots"));
        assertFalse(adminTools.contains("manage_notifications"));
        assertTrue(managerTools.contains("delete_contact"));
        assertFalse(managerTools.contains("manage_notifications"));
    }

    @Test
    @DisplayName("toolsForRole(null) возвращает CLIENT набор")
    void toolsForRoleNullReturnsClient() {
        java.util.Set<String> tools = crmToolService.toolsForRole(null);
        assertFalse(tools.contains("create_contact"));
        assertTrue(tools.contains("get_branch_staff_slots"));
    }

    @Test
    @DisplayName("resolve_branch присутствует в схемах")
    void toolSchemaResolveBranchExists() {
        boolean found = crmToolService.getToolSchemas().stream()
            .map(s -> (Map<String, Object>) s.get("function"))
            .anyMatch(f -> "resolve_branch".equals(f.get("name")));
        assertTrue(found, "resolve_branch tool should be present");
    }

    @Test
    @DisplayName("executeTool resolve_branch вызывает POST /branches/resolve")
    void resolveBranchPostsToResolveEndpoint() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{\"matched\":true,\"branchId\":\"abc-123\"}");

        Map<String, Object> args = Map.of("name", "виртуальный");
        String result = crmToolService.executeTool("resolve_branch", args, "t1", Map.of());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).postForObject(urlCap.capture(), any(HttpEntity.class), eq(String.class));
        assertTrue(urlCap.getValue().endsWith("/branches/resolve"));
        assertNotNull(result);
    }

    @Test
    @DisplayName("validateIdArg отклоняет 'virtual' (не UUID)")
    void validateIdArgRejectsNonUuid() {
        Map<String, Object> args = Map.of("branch_id", "virtual");
        String result = crmToolService.executeTool("get_branch_staff_slots", args, "t1", Map.of());
        assertTrue(result.contains("error") && result.contains("UUID"));
        verifyNoInteractions(rest);
    }

    @Test
    @DisplayName("validateIdArg принимает валидный UUID")
    void validateIdArgAcceptsUuid() {
        String uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{}");
        Map<String, Object> args = Map.of("branch_id", uuid, "date", "tomorrow");
        String result = crmToolService.executeTool("get_branch_staff_slots", args, "t1", Map.of());
        verify(rest).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("catch returns valid JSON with hint and recoverable")
    void catchReturnsValidJsonWithHint() {
        Map<String, Object> args = Map.of("contact_id", "not-a-uuid-just-text");
        String result = crmToolService.executeTool("get_contact", args, "t1", Map.of());
        assertNotNull(result);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("\"hint\""));
        assertTrue(result.contains("\"recoverable\""));
        assertFalse(result.contains("\\n\\n"));
    }

    @Test
    @DisplayName("resolve_branch matched:false возвращается как есть")
    void resolveBranchMatchedFalse() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{\"matched\":false}");
        String result = crmToolService.executeTool("resolve_branch", Map.of("name", "xxx"), "t1", Map.of());
        assertTrue(result.contains("\"matched\":false"));
    }
}