package com.tryneuro.aibot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ModelResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final CrmToolService toolService;
    private final UserConfigService userConfigService;
    private final ObjectMapper mapper;
    private final MapResolverService actorResolver;
    private final RagService ragService;

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

    @PostConstruct
    void configureProxy() {
        String proxyUrl = System.getenv("OPENROUTER_PROXY");
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            proxyUrl = System.getenv("TELEGRAM_PROXY");
        }
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            try {
                URI uri = URI.create(proxyUrl.startsWith("http") ? proxyUrl : "http://" + proxyUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 8888;
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                ProxySelector defaultSelector = ProxySelector.getDefault();

                ProxySelector.setDefault(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI targetUri) {
                        if (targetUri.getHost() != null && targetUri.getHost().contains("openrouter.ai")) {
                            log.debug("Proxy routing {} via {}:{}", targetUri, host, port);
                            return List.of(proxy);
                        }
                        return defaultSelector != null
                            ? defaultSelector.select(targetUri)
                            : List.of(Proxy.NO_PROXY);
                    }

                    @Override
                    public void connectFailed(URI targetUri, SocketAddress sa, IOException ioe) {
                        log.warn("Proxy connect failed to {}: {}", targetUri, ioe.getMessage());
                    }
                });

                log.info("OpenRouter proxy enabled: {}:{} (env: OPENROUTER_PROXY={}, TELEGRAM_PROXY={})",
                    host, port,
                    System.getenv("OPENROUTER_PROXY") != null ? "set" : "not set",
                    System.getenv("TELEGRAM_PROXY") != null ? "set" : "not set");
            } catch (Exception e) {
                log.warn("Failed to parse proxy {}: {}", proxyUrl, e.getMessage());
            }
        } else {
            log.info("No proxy configured for OpenRouter — direct connection");
        }
    }

    @SuppressWarnings("unchecked")
    public String processMessage(List<Map<String, String>> history, long chatId) {
        log.info("processMessage chat_id={}, history_size={}", chatId, history.size());

        UserConfigService.UserConfig cfg = userConfigService.getConfig(chatId);
        if (cfg == null || cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            log.warn("processMessage chat_id={}: no API key configured", chatId);
            return "У вас не настроен API ключ для нейросети.\n"
                + "Перейдите в CRM → AI Настройки, сохраните ваш OpenRouter API key и Telegram ID.";
        }
        log.info("processMessage chat_id={}: apiKey={}..., model={}",
            chatId, cfg.apiKey().substring(0, Math.min(8, cfg.apiKey().length())), cfg.llmModel());

        Map<String, String> actor = actorResolver.resolveActor(chatId);
        String tenantId = actor.get("tenant_id");
        String role = actor.getOrDefault("role", "CLIENT");
        String modelName = cfg.llmModel() != null && !cfg.llmModel().isEmpty()
            ? cfg.llmModel() : "openrouter/auto";
        log.info("processMessage chat_id={}: role={}, tenantId={}, model={}", chatId, role, tenantId, modelName);

        String systemPrompt = buildSystemPrompt(role);

        String lastUserQuery = history.isEmpty() ? "" :
            history.get(history.size() - 1).getOrDefault("content", "");
        log.info("processMessage chat_id={}: calling RAG with query=\"{}\"", chatId, lastUserQuery);
        String ragContext = ragService.enhancePrompt(tenantId, lastUserQuery);
        if (!ragContext.isEmpty()) {
            systemPrompt += ragContext;
            log.info("processMessage chat_id={}: RAG context added, len={}", chatId, ragContext.length());
        } else {
            log.info("processMessage chat_id={}: no RAG context", chatId);
        }

        List<Message> historyMessages = buildHistoryMessages(history);
        log.info("processMessage chat_id={}: calling OpenRouter model={}, messages_count={}",
            chatId, modelName, historyMessages.size());

        try {
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(cfg.apiKey())
                    .model(modelName)
                    .build())
                .build();

            ChatClient chatClient = ChatClient.builder(chatModel).build();

            List<FunctionToolCallback<Map<String, Object>, String>> callbacks = buildCallbacks(tenantId, actor);
            log.info("processMessage chat_id={}: {} tool callbacks registered", chatId, callbacks.size());

            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                .system(systemPrompt)
                .messages(historyMessages)
                .tools(callbacks.toArray());

            long openrouterStart = System.currentTimeMillis();
            String content = request.call().content();
            long openrouterElapsed = System.currentTimeMillis() - openrouterStart;

            log.info("processMessage chat_id={}: OpenRouter responded in {}ms, content len={}",
                chatId, openrouterElapsed, content != null ? content.length() : 0);
            if (content != null && !content.isEmpty()) {
                log.debug("processMessage chat_id={}: response preview: {}", chatId,
                    content.substring(0, Math.min(200, content.length())));
            }
            return content != null ? content : "";

        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : "";
            log.error("processMessage chat_id={} model={}: exception: {}", chatId, modelName, errMsg, e);

            if (errMsg.contains("403")) {
                return "Модель \"" + modelName + "\" недоступна. Проверьте API-ключ и баланс на OpenRouter.\n"
                    + "Сменить модель можно в CRM → AI Настройки.";
            }
            if (errMsg.contains("429")) {
                return "Слишком много запросов к нейросети. Попробуйте через минуту.";
            }
            if (errMsg.contains("502") || errMsg.contains("503") || errMsg.contains("500")) {
                return "Сервер ИИ временно недоступен. Повторите попытку позже.";
            }
            return "Ошибка нейросети: " + e.getMessage();
        }
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

    private List<Message> buildHistoryMessages(List<Map<String, String>> history) {
        List<Message> messages = new ArrayList<>();
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

    private List<FunctionToolCallback<Map<String, Object>, String>> buildCallbacks(
            String tenantId, Map<String, String> actor) {
        List<FunctionToolCallback<Map<String, Object>, String>> callbacks = new ArrayList<>();
        for (CrmToolService.ToolDef def : toolService.getToolDefinitions()) {
            FunctionToolCallback<Map<String, Object>, String> cb = FunctionToolCallback
                .builder(def.name(), (Map<String, Object> args) ->
                    toolService.executeTool(def.name(), args, tenantId, actor))
                .description(def.description())
                .inputType((Class<Map<String, Object>>) (Class<?>) Map.class)
                .build();
            callbacks.add(cb);
        }
        return callbacks;
    }
}
