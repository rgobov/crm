package com.tryneuro.aibot.service;

import chat.giga.client.GigaChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AiAgentServiceTest {

    @Mock private CrmToolService toolService;
    @Mock private UserConfigService userConfigService;
    @Mock private MapResolverService actorResolver;
    @Mock private RagService ragService;
    @Mock private GigaChatClient gigaClient;

    private AiAgentService aiAgentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        aiAgentService = new AiAgentService(toolService, userConfigService, objectMapper,
                actorResolver, ragService, "GIGACHAT_API_PERS", 15, 90000L);

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
}