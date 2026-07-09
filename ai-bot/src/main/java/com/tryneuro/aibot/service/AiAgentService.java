package com.tryneuro.aibot.service;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatFunction;
import chat.giga.model.completion.ChatFunctionParameters;
import chat.giga.model.completion.ChatFunctionParametersProperty;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final CrmToolService toolService;
    private final UserConfigService userConfigService;
    private final ObjectMapper mapper;
    private final MapResolverService actorResolver;
    private final RagService ragService;
    private final String scopeString;

    private final ConcurrentHashMap<String, GigaChatClient> clientCache = new ConcurrentHashMap<>();

    private static final int MAX_REACT_ITERATIONS = 8;
    private static final long REACT_TIMEOUT_MS = 30_000;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        Ты — AI-ассистент CRM системы TryNeuro.
        Помогаешь клиентам с контактами, записями, услугами.
        Отвечаешь кратко, по делу, на русском языке.

        Используй инструменты CRM для работы с данными. Не выдумывай информацию — используй поиск.

        Правила:
        - Для сложных многошаговых задач сначала вызови get_instructions
        - Используй search_contacts для поиска клиентов, create_contact для создания
        - Если что-то не найдено — ищи шире (пустой query) или предложи создать (если есть права)
        - После получения результатов инструментов — проанализируй и реши, нужны ли ещё шаги
        - Когда задача выполнена — дай ответ пользователю
        """;

    public AiAgentService(CrmToolService toolService, UserConfigService userConfigService,
                          ObjectMapper mapper, MapResolverService actorResolver,
                          RagService ragService,
                          @Value("${gigachat.scope:GIGACHAT_API_PERS}") String scopeString) {
        this.toolService = toolService;
        this.userConfigService = userConfigService;
        this.mapper = mapper;
        this.actorResolver = actorResolver;
        this.ragService = ragService;
        this.scopeString = scopeString;
    }

    private GigaChatClient getOrCreateClient(String authKey) {
        return clientCache.computeIfAbsent(authKey, key ->
            GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClientBuilder.builder()
                    .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                        .scope(Scope.valueOf(scopeString))
                        .authKey(key)
                        .verifySslCerts(false)
                        .build())
                    .build())
                .build()
        );
    }

    @SuppressWarnings("unchecked")
    public String processMessage(List<Map<String, String>> history, long chatId) {
        log.info("processMessage chat_id={}, history_size={}", chatId, history.size());

        UserConfigService.UserConfig cfg = userConfigService.getConfig(chatId);
        if (cfg == null || cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            log.warn("processMessage chat_id={}: no API key configured", chatId);
            return "⚠️ Не настроен API-ключ GigaChat. Укажите его в CRM → AI Настройки.";
        }
        log.info("processMessage chat_id={}: apiKey={}..., model={}",
            chatId, cfg.apiKey().substring(0, Math.min(8, cfg.apiKey().length())), cfg.llmModel());

        Map<String, String> actor = actorResolver.resolveActor(chatId);
        String tenantId = actor.get("tenant_id");
        String role = actor.getOrDefault("role", "CLIENT");
        String modelName = cfg.llmModel() != null && !cfg.llmModel().isEmpty()
                ? cfg.llmModel() : "GigaChat";
        log.info("processMessage chat_id={}: role={}, tenantId={}, model={}", chatId, role, tenantId, modelName);

        Map<String, String> actorHeaders = new LinkedHashMap<>();
        actorHeaders.put("X-Actor-Role", role);
        actorHeaders.put("X-Actor-Contact-Id", actor.getOrDefault("contact_id", ""));
        actorHeaders.put("X-Actor-Staff-Id", actor.getOrDefault("staff_id", ""));

        String systemPrompt = buildSystemPrompt(role);

        String lastUserQuery = history.isEmpty() ? "" :
                history.get(history.size() - 1).getOrDefault("content", "");
        String ragContext = ragService.enhancePrompt(tenantId, lastUserQuery);
        if (!ragContext.isEmpty()) {
            systemPrompt += ragContext;
            log.info("processMessage chat_id={}: RAG context added, len={}", chatId, ragContext.length());
        } else {
            log.info("processMessage chat_id={}: no RAG context", chatId);
        }

        List<ChatMessage> messages = buildGigaChatMessages(history);
        List<ChatFunction> functions = buildGigaChatFunctions(tenantId, actorHeaders);
        log.info("processMessage chat_id={}: {} chat functions registered", chatId, functions.size());

        GigaChatClient client = getOrCreateClient(cfg.apiKey());

        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < MAX_REACT_ITERATIONS; iter++) {
            if (System.currentTimeMillis() - startTime > REACT_TIMEOUT_MS) {
                log.warn("processMessage chat_id={}: ReAct timeout after {} iterations", chatId, iter);
                return "⚠️ Превышено время ожидания ответа. Попробуйте упростить запрос.";
            }

            try {
                CompletionRequest request = CompletionRequest.builder()
                        .model(modelName)
                        .messages(prependSystem(systemPrompt, messages))
                        .functions(functions)
                        .functionCall("auto")
                        .build();

                long gigaStart = System.currentTimeMillis();
                CompletionResponse response = client.completions(request);
                long gigaElapsed = System.currentTimeMillis() - gigaStart;

                if (response.choices() == null || response.choices().isEmpty()) {
                    log.warn("processMessage chat_id={}: empty choices", chatId);
                    return "⚠️ Пустой ответ от модели GigaChat.";
                }

                ChatMessage responseMsg = ChatMessage.of(response.choices().get(0).message());
                log.info("processMessage chat_id={}: GigaChat responded in {}ms, role={}, hasFunctionCall={}",
                        chatId, gigaElapsed, responseMsg.role(),
                        responseMsg.functionCall() != null);

                if (responseMsg.functionCall() != null) {
                    messages.add(responseMsg);

                    String toolName = responseMsg.functionCall().name();
                    Map<String, Object> toolArgs = responseMsg.functionCall().arguments();
                    log.info("processMessage chat_id={}: tool call: name={}, args={}", chatId, toolName, toolArgs);

                    String toolResult = toolService.executeTool(toolName, toolArgs, tenantId, actorHeaders);
                    log.info("processMessage chat_id={}: tool result len={}", chatId, toolResult != null ? toolResult.length() : 0);

                    messages.add(ChatMessage.builder()
                            .role(ChatMessageRole.FUNCTION)
                            .name(toolName)
                            .content(toolResult != null ? toolResult : "{}")
                            .build());
                } else {
                    String content = responseMsg.content();
                    log.info("processMessage chat_id={}: final response, len={}", chatId, content != null ? content.length() : 0);
                    return content != null ? content : "";
                }
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "";
                log.error("processMessage chat_id={} model={}: exception at iter {}: {}", chatId, modelName, iter, errMsg, e);

                if (errMsg.contains("401") || errMsg.contains("Unauthorized")) {
                    return "⚠️ Неверный API-ключ GigaChat. Проверьте ключ в CRM → AI Настройки.";
                }
                if (errMsg.contains("429") || errMsg.contains("Too Many Requests")) {
                    return "⚠️ Слишком много запросов к GigaChat. Попробуйте через минуту.";
                }
                if (errMsg.contains("5") && (errMsg.contains("500") || errMsg.contains("502") || errMsg.contains("503"))) {
                    return "⚠️ Сервер GigaChat временно недоступен. Повторите попытку позже.";
                }
                return "⚠️ Ошибка нейросети: " + e.getMessage();
            }
        }

        log.warn("processMessage chat_id={}: ReAct loop exhausted", chatId);
        return "⚠️ Не удалось получить ответ за допустимое количество шагов.";
    }

    private List<ChatMessage> buildGigaChatMessages(List<Map<String, String>> history) {
        List<ChatMessage> messages = new ArrayList<>();
        for (Map<String, String> msg : history) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            if ("user".equals(role)) {
                messages.add(ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(content)
                        .build());
            } else if ("assistant".equals(role)) {
                messages.add(ChatMessage.builder()
                        .role(ChatMessageRole.ASSISTANT)
                        .content(content)
                        .build());
            }
        }
        return messages;
    }

    private List<ChatMessage> prependSystem(String systemPrompt, List<ChatMessage> messages) {
        if (systemPrompt == null || systemPrompt.isEmpty()) return messages;
        List<ChatMessage> result = new ArrayList<>();
        result.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content(systemPrompt)
                .build());
        result.addAll(messages);
        return result;
    }

    private List<ChatFunction> buildGigaChatFunctions(String tenantId, Map<String, String> actorHeaders) {
        List<ChatFunction> functions = new ArrayList<>();
        for (CrmToolService.ToolDef def : toolService.getToolDefinitions()) {
            functions.add(toChatFunction(def));
        }
        return functions;
    }

    ChatFunction toChatFunction(CrmToolService.ToolDef def) {
        ChatFunction.ChatFunctionBuilder builder = ChatFunction.builder()
                .name(def.name())
                .description(def.description());

        Map<String, Object> paramSchema = def.parameters();
        if (paramSchema != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) paramSchema.get("properties");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) paramSchema.get("required");

            ChatFunctionParameters.ChatFunctionParametersBuilder paramsBuilder = ChatFunctionParameters.builder();
            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String propName = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                    String type = (String) propDef.get("type");
                    String description = (String) propDef.get("description");
                    paramsBuilder.property(propName, ChatFunctionParametersProperty.builder()
                            .type(type)
                            .description(description)
                            .build());
                }
            }
            if (required != null) {
                paramsBuilder.required(required);
            }
            builder.parameters(paramsBuilder.build());
        }

        return builder.build();
    }

    private String buildSystemPrompt(String role) {
        String prompt = SYSTEM_PROMPT_TEMPLATE;
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            prompt += "\nТвоя роль: " + role + ". У тебя полный доступ ко всем функциям CRM.";
        } else if ("EMPLOYEE".equals(role)) {
            prompt += "\nТвоя роль: " + role
                    + ". Ты можешь создавать записи для любых клиентов, "
                    + "искать контакты, просматривать расписание и филиалы, "
                    + "но отменять можешь только свои записи.";
        } else {
            prompt += "\nТвоя роль: " + role + ". Ты можешь управлять только своими данными.";
        }
        return prompt;
    }
}