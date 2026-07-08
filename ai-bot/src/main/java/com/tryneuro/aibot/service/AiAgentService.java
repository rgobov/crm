package com.tryneuro.aibot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final CrmToolService toolService;
    private final UserConfigService userConfigService;
    private final ObjectMapper mapper;
    private final MapResolverService actorResolver;
    private final RagService ragService;

    private static final int MAX_RETRIES = 3;

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
                          RagService ragService) {
        this.toolService = toolService;
        this.userConfigService = userConfigService;
        this.mapper = mapper;
        this.actorResolver = actorResolver;
        this.ragService = ragService;
    }

    public String processMessage(List<Map<String, String>> history, long chatId) {
        UserConfigService.UserConfig cfg = userConfigService.getConfig(chatId);
        if (cfg == null || cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            return "У вас не настроен API ключ для нейросети.\n"
                + "Перейдите в CRM → AI Настройки, сохраните ваш OpenRouter API key и Telegram ID.";
        }

        Map<String, String> actor = actorResolver.resolveActor(chatId);
        String tenantId = actor.get("tenant_id");
        String role = actor.getOrDefault("role", "CLIENT");
        String modelName = cfg.llmModel() != null && !cfg.llmModel().isEmpty()
            ? cfg.llmModel() : "openrouter/auto";

        String systemPrompt = buildSystemPrompt(role);

        String lastUserQuery = history.isEmpty() ? "" :
            history.get(history.size() - 1).getOrDefault("content", "");
        String ragContext = ragService.enhancePrompt(tenantId, lastUserQuery);
        if (!ragContext.isEmpty()) {
            systemPrompt += ragContext;
        }

        List<Message> messages = buildMessages(systemPrompt, history);

        OpenAiApi openAiApi = new OpenAiApi("https://openrouter.ai/api/v1", cfg.apiKey());

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                List<FunctionCallbackWrapper<String, String>> toolCallbacks = buildCallbacks(tenantId, actor);

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(modelName)
                    .withFunctionCallbacks(new ArrayList<>(toolCallbacks))
                    .build();

                OpenAiChatModel model = new OpenAiChatModel(openAiApi, options);
                Prompt prompt = new Prompt(messages, options);
                ChatResponse response = model.call(prompt);

                Generation gen = response.getResult();
                String content = gen.getOutput().getContent();
                return content != null ? content : "";
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "";
                boolean retryable = errMsg.contains("502") || errMsg.contains("503")
                    || errMsg.contains("500") || errMsg.contains("429");
                if (attempt < MAX_RETRIES - 1 && retryable) {
                    log.warn("Retry {}/{} model={} chat_id={}: {}",
                        attempt + 1, MAX_RETRIES, modelName, chatId, errMsg);
                    try { Thread.sleep(2000L * (attempt + 1)); } catch (InterruptedException ignored) {}
                    continue;
                }
                log.error("Error model={} chat_id={}: {}", modelName, chatId, errMsg);
                if (errMsg.contains("403")) {
                    return "Модель \"" + modelName + "\" недоступна. Проверьте API-ключ и баланс на OpenRouter.\n"
                        + "Сменить модель можно в CRM → AI Настройки.";
                }
                if (errMsg.contains("429")) {
                    return "Слишком много запросов к нейросети. Попробуйте через минуту.";
                }
                if (retryable) {
                    return "Сервер ИИ временно недоступен. Повторите попытку позже.";
                }
                return "Ошибка нейросети: " + e.getMessage();
            }
        }
        return "Сервер ИИ временно недоступен. Повторите попытку позже.";
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

    private List<Message> buildMessages(String systemPrompt, List<Map<String, String>> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (Map<String, String> msg : history) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            }
        }
        return messages;
    }

    private List<FunctionCallbackWrapper<String, String>> buildCallbacks(
            String tenantId, Map<String, String> actor) {
        List<FunctionCallbackWrapper<String, String>> callbacks = new ArrayList<>();
        for (Map<String, Object> schema : toolService.getToolSchemas()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) schema.get("function");
            String name = (String) function.get("name");
            String description = (String) function.get("description");

            FunctionCallbackWrapper<String, String> cb = FunctionCallbackWrapper
                .<String, String>builder(jsonArgs -> {
                    try {
                        Map<String, Object> args = mapper.readValue(jsonArgs,
                            new TypeReference<Map<String, Object>>() {});
                        return toolService.executeTool(name, args, tenantId, actor);
                    } catch (Exception e) {
                        log.error("Tool {} error: {}", name, e.getMessage());
                        return "{\"error\":\"" + e.getMessage() + "\"}";
                    }
                })
                .withName(name)
                .withDescription(description)
                .withInputType(String.class)
                .withResponseConverter(Function.identity())
                .build();

            callbacks.add(cb);
        }
        return callbacks;
    }
}
