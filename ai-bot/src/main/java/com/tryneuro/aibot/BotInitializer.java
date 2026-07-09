package com.tryneuro.aibot;

import com.tryneuro.aibot.service.AiAgentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class BotInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final List<TryNeuroBot> bots = new ArrayList<>();
    private final AiAgentService aiAgent;
    private TelegramBotsApi api;

    public BotInitializer(AiAgentService aiAgent) {
        this.aiAgent = aiAgent;
    }

    @PostConstruct
    public void registerBots() {
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String token = System.getenv("BOT_TOKEN_" + i);
            if (token != null && !token.isEmpty()) {
                tokens.add(token);
            }
        }

        if (tokens.isEmpty()) {
            log.warn("No BOT_TOKEN_1..4 found — bots not registered");
            return;
        }

        DefaultBotOptions options = new DefaultBotOptions();
        String proxy = System.getenv("TELEGRAM_PROXY");
        if (proxy != null && !proxy.isEmpty()) {
            try {
                URI uri = URI.create(proxy);
                options.setProxyType(DefaultBotOptions.ProxyType.HTTP);
                options.setProxyHost(uri.getHost());
                options.setProxyPort(uri.getPort() > 0 ? uri.getPort() : 8888);
                log.info("Proxy configured: {}:{}", uri.getHost(), options.getProxyPort());
            } catch (Exception e) {
                log.warn("Failed to parse TELEGRAM_PROXY: {}", proxy);
            }
        }

        try {
            api = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            log.error("Failed to initialize TelegramBotsApi", e);
            return;
        }

        for (int i = 0; i < tokens.size(); i++) {
            String username = "NineCRM_AI_" + (i + 1) + "_bot";
            TryNeuroBot bot = new TryNeuroBot(tokens.get(i), username, i + 1, options, aiAgent);
            try {
                api.registerBot(bot);
                bots.add(bot);

                try {
                    bot.execute(new SetMyCommands(
                        List.of(
                            new BotCommand("/start", "Начать диалог"),
                            new BotCommand("/new", "Новый диалог"),
                            new BotCommand("/help", "Справка")
                        ),
                        new BotCommandScopeDefault(), null
                    ));
                } catch (TelegramApiException e) {
                    log.warn("Bot {} failed to set commands: {}", i + 1, e.getMessage());
                }

                log.info("Bot {} registered as {}", i + 1, username);
            } catch (TelegramApiException e) {
                log.error("Bot {} failed to register: {}", i + 1, e.getMessage());
            }
        }
        log.info("Bot registration complete: {} of {} bots registered", bots.size(), tokens.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {} bots...", bots.size());
    }
}
