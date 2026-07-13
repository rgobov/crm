package com.tryneuro.aibot.service;

import chat.giga.client.GigaChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiAgentServiceTest {

    @Mock private CrmToolService toolService;
    @Mock private UserConfigService userConfigService;
    @Mock private MapResolverService actorResolver;
    @Mock private RagService ragService;
    @Mock private RestTemplate restTemplate;
    @Mock private GigaChatClient gigaClient;

    private AiAgentService aiAgentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        aiAgentService = new AiAgentService(toolService, userConfigService, objectMapper,
                actorResolver, ragService, restTemplate, "GIGACHAT_API_PERS", 15, 90000L, "http://localhost:8083");

        when(toolService.getToolDefinitions()).thenReturn(buildSampleDefs());
        when(toolService.toolsForRole("CLIENT")).thenReturn(java.util.Set.of(
                "search_services", "create_appointment", "get_branches", "get_branch_staff_slots"));
        when(toolService.toolsForRole("ADMIN")).thenReturn(java.util.Set.of(
                "search_contacts", "create_contact", "search_services", "create_appointment",
                "get_branches", "get_branch_staff_slots", "get_report"));
    }

    private List<CrmToolService.ToolDef> buildSampleDefs() {
        List<CrmToolService.ToolDef> defs = new ArrayList<>();
        for (String name : java.util.List.of(
                "search_contacts", "create_contact", "search_services", "create_appointment",
                "get_branches", "get_branch_staff_slots", "get_report", "manage_notifications")) {
            defs.add(new CrmToolService.ToolDef(name, "desc", java.util.Map.of(
                    "type", "object", "properties", java.util.Map.of(), "required", java.util.List.of())));
        }
        return defs;
    }

    private List<Map<String, String>> makeHistory(String userMsg) {
        return List.of(Map.of("role", "user", "content", userMsg));
    }

    private void mockLocalConfig() {
        when(userConfigService.getConfig(anyLong())).thenReturn(
                new UserConfigService.UserConfig("", "GigaChat", "local"));
        when(actorResolver.resolveActor(anyLong())).thenReturn(
                Map.of("role", "CLIENT", "tenant_id", "tenant-1", "contact_id", "", "staff_id", ""));
        when(ragService.enhancePrompt(anyString(), anyString())).thenReturn("");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> textResponse(String text) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", text);
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("message", msg);
        choice.put("finish_reason", "stop");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("choices", List.of(choice));
        body.put("id", "test");
        body.put("model", "qwen");
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> toolCallResponse(String toolName, String argsJson, String callId) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", toolName);
        function.put("arguments", argsJson);
        Map<String, Object> tc = new LinkedHashMap<>();
        tc.put("id", callId);
        tc.put("type", "function");
        tc.put("function", function);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", null);
        msg.put("tool_calls", List.of(tc));
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("message", msg);
        choice.put("finish_reason", "tool_calls");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("choices", List.of(choice));
        body.put("id", "test");
        body.put("model", "qwen");
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @Test
    @DisplayName("buildGigaChatFunctions(CLIENT) возвращает меньше функций чем (ADMIN)")
    void buildGigaChatFunctionsClientHasFewerThanAdmin() {
        int clientCount = aiAgentService.buildGigaChatFunctions("CLIENT").size();
        int adminCount = aiAgentService.buildGigaChatFunctions("ADMIN").size();
        assertTrue(clientCount < adminCount,
                "CLIENT should have fewer functions than ADMIN: " + clientCount + " vs " + adminCount);
        assertEquals(4, clientCount);
        assertEquals(7, adminCount);
    }

    @Test
    @DisplayName("buildGigaChatFunctions(CLIENT) не содержит create_contact и get_report")
    void buildGigaChatFunctionsClientExcludesAdminTools() {
        List<chat.giga.model.completion.ChatFunction> clientFns = aiAgentService.buildGigaChatFunctions("CLIENT");
        List<String> names = clientFns.stream().map(chat.giga.model.completion.ChatFunction::name).toList();
        assertFalse(names.contains("create_contact"));
        assertFalse(names.contains("get_report"));
        assertFalse(names.contains("manage_notifications"));
        assertTrue(names.contains("get_branch_staff_slots"));
    }

    @Test
    @DisplayName("tryPartialFallback возвращает null когда messages пустой/один (без вызова GigaChat)")
    void tryPartialFallbackReturnsNullForEmptyMessages() {
        String result = aiAgentService.tryPartialFallback(gigaClient, "GigaChat", "system",
                new ArrayList<>(), 123L);
        assertNull(result);
    }

    @Test
    @DisplayName("tryPartialFallback возвращает null когда messages содержит только один элемент")
    void tryPartialFallbackReturnsNullForSingleMessage() {
        java.util.List<chat.giga.model.completion.ChatMessage> msgs = new ArrayList<>();
        msgs.add(chat.giga.model.completion.ChatMessage.builder()
                .role(chat.giga.model.completion.ChatMessageRole.USER)
                .content("hi")
                .build());
        String result = aiAgentService.tryPartialFallback(gigaClient, "GigaChat", "system", msgs, 123L);
        assertNull(result);
    }

    @Test
    @DisplayName("Итерации и таймаут берутся из @Value (15 и 90000 по умолчанию)")
    void iterationsAndTimeoutFromValue() {
        assertEquals(15, ReflectionTestUtils.getField(aiAgentService, "maxReactIterations"));
        assertEquals(90000L, ReflectionTestUtils.getField(aiAgentService, "reactTimeoutMs"));
    }

    @Test
    @DisplayName("processMessage с local провайдером возвращает текст из LLM")
    void processMessageLocalReturnsText() {
        mockLocalConfig();
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(textResponse("Привет, чем могу помочь?"));

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertEquals("Привет, чем могу помочь?", result);
    }

    @Test
    @DisplayName("processMessage с local: tool_call → executeTool → финальный ответ")
    void processMessageLocalToolCallThenText() {
        mockLocalConfig();
        String args = "{\"query\":\"услуги\"}";
        when(toolService.executeTool(anyString(), anyMap(), anyString(), anyMap()))
                .thenReturn("Найденные услуги: стрижка");
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(toolCallResponse("search_services", args, "call_1"),
                           textResponse("Вот услуги: стрижка"));

        String result = aiAgentService.processMessage(makeHistory("какие услуги?"), 123L);
        assertEquals("Вот услуги: стрижка", result);
    }

    @Test
    @DisplayName("processMessage с local: пустые choices → ошибка")
    void processMessageLocalEmptyChoices() {
        mockLocalConfig();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("choices", List.of());
        body.put("id", "test");
        body.put("model", "qwen");
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertTrue(result.contains("Пустой"));
    }

    @Test
    @DisplayName("processMessage с local: RestTemplate ошибка → сообщение об ошибке")
    void processMessageLocalRestTemplateError() {
        mockLocalConfig();
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertTrue(result.contains("Ошибка"));
    }

    @Test
    @DisplayName("processMessage без конфига → сообщение об ошибке")
    void processMessageNoConfig() {
        when(userConfigService.getConfig(anyLong())).thenReturn(null);
        when(actorResolver.resolveActor(anyLong())).thenReturn(
                Map.of("role", "CLIENT", "tenant_id", "tenant-1", "contact_id", "", "staff_id", ""));
        when(ragService.enhancePrompt(anyString(), anyString())).thenReturn("");

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertTrue(result.contains("Настройки"));
    }

    @Test
    @DisplayName("processMessage с gigachat без api_key → сообщение об ошибке")
    void processMessageGigachatNoApiKey() {
        when(userConfigService.getConfig(anyLong())).thenReturn(
                new UserConfigService.UserConfig("", "GigaChat", "gigachat"));
        when(actorResolver.resolveActor(anyLong())).thenReturn(
                Map.of("role", "CLIENT", "tenant_id", "tenant-1", "contact_id", "", "staff_id", ""));
        when(ragService.enhancePrompt(anyString(), anyString())).thenReturn("");

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertTrue(result.contains("ключ"));
    }

    @Test
    @DisplayName("processMessage с local: невалидный JSON в tool_call → пропускает и продолжает")
    void processMessageLocalInvalidToolArgs() {
        mockLocalConfig();
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(toolCallResponse("search_services", "{bad json}", "call_1"),
                           textResponse("Извините, не удалось"));

        String result = aiAgentService.processMessage(makeHistory("услуги"), 123L);
        assertEquals("Извините, не удалось", result);
    }

    @Test
    @DisplayName("processMessage с gigachat с api_key не падает")
    void processMessageGigachatWithApiKeyDoesNotThrow() {
        when(userConfigService.getConfig(anyLong())).thenReturn(
                new UserConfigService.UserConfig("test-api-key", "GigaChat", "gigachat"));
        when(actorResolver.resolveActor(anyLong())).thenReturn(
                Map.of("role", "CLIENT", "tenant_id", "tenant-1", "contact_id", "", "staff_id", ""));
        when(ragService.enhancePrompt(anyString(), anyString())).thenReturn("");

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("processMessage с local: максимальное число итераций → fallback")
    void processMessageLocalMaxIterations() {
        mockLocalConfig();
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(toolCallResponse("search_services", "{}", "call_1"));
        when(toolService.executeTool(anyString(), anyMap(), anyString(), anyMap()))
                .thenReturn("результат");

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertTrue(result.contains("шагов"));
    }

    @Test
    @DisplayName("processMessage с local: переопределение модели из конфига")
    void processMessageLocalModelFromConfig() {
        when(userConfigService.getConfig(anyLong())).thenReturn(
                new UserConfigService.UserConfig("", "custom-model", "local"));
        when(actorResolver.resolveActor(anyLong())).thenReturn(
                Map.of("role", "CLIENT", "tenant_id", "tenant-1", "contact_id", "", "staff_id", ""));
        when(ragService.enhancePrompt(anyString(), anyString())).thenReturn("");
        when(restTemplate.postForEntity(contains("/v1/chat/completions"), any(), eq(Map.class)))
                .thenReturn(textResponse("Ответ от custom-model"));

        String result = aiAgentService.processMessage(makeHistory("тест"), 123L);
        assertEquals("Ответ от custom-model", result);
    }
}