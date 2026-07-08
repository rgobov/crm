package com.tryneuro.aibot;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class BotInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final List<String> botTokens;
    private final DefaultBotOptions options;

    public BotInitializer() {
        botTokens = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String token = System.getenv("BOT_TOKEN_" + i);
            if (token != null && !token.isEmpty()) {
                botTokens.add(token);
            }
        }

        options = new DefaultBotOptions();
        String proxy = System.getenv("TELEGRAM_PROXY");
        if (proxy != null && !proxy.isEmpty()) {
            try {
                URI uri = URI.create(proxy);
                options.setProxyType(DefaultBotOptions.ProxyType.HTTP);
                options.setProxyHost(uri.getHost());
                options.setProxyPort(uri.getPort() > 0 ? uri.getPort() : 8888);
                log.info("Proxy configured: {}:{}", uri.getHost(), uri.getPort());
            } catch (Exception e) {
                log.warn("Failed to parse TELEGRAM_PROXY: {}", proxy);
            }
        }
    }

    @PostConstruct
    public void registerBots() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            for (int i = 0; i < botTokens.size(); i++) {
                String username = "NineCRM_AI_" + (i + 1) + "_bot";
                TryNeuroBot bot = new TryNeuroBot(botTokens.get(i), username, i + 1, options);
                api.registerBot(bot);
                log.info("Bot {} registered as {}", i + 1, username);
            }
            log.info("All {} bots registered successfully", botTokens.size());
        } catch (TelegramApiException e) {
            log.error("Failed to register bots: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
