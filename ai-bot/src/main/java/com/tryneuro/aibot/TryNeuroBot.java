package com.tryneuro.aibot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TryNeuroBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TryNeuroBot.class);

    private final String token;
    private final String username;
    private final int index;

    public TryNeuroBot(String token, String username, int index, DefaultBotOptions options) {
        super(options);
        this.token = token;
        this.username = username;
        this.index = index;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            log.info("Bot {} received from {}: {}", index, chatId, text);

            SendMessage response = new SendMessage();
            response.setChatId(chatId);
            response.setText("Привет! Я AI-ассистент CRM TryNeuro (бот " + index + ").\nAI-функции подключаются следующим шагом.");

            try {
                execute(response);
            } catch (TelegramApiException e) {
                log.error("Bot {} failed to reply: {}", index, e.getMessage());
            }
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
