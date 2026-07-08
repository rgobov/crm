package com.tryneuro.aibot;

import com.tryneuro.aibot.service.AiAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TryNeuroBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TryNeuroBot.class);

    private final String token;
    private final String username;
    private final int index;
    private final AiAgentService aiAgent;

    private final Map<Long, List<Map<String, String>>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_EXCHANGES = 10;

    public TryNeuroBot(String token, String username, int index, DefaultBotOptions options,
                       AiAgentService aiAgent) {
        super(options);
        this.token = token;
        this.username = username;
        this.index = index;
        this.aiAgent = aiAgent;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().strip();

        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            handleCommand(chatId, text);
            return;
        }

        log.info("Bot {} received from {}: {}", index, chatId, text);

        try {
            SendChatAction action = new SendChatAction();
            action.setChatId(chatId.toString());
            action.setAction(ActionType.TYPING);
            execute(action);
        } catch (TelegramApiException e) {
            log.warn("Bot {} failed to send typing: {}", index, e.getMessage());
        }

        conversations.computeIfAbsent(chatId, k -> new ArrayList<>());
        List<Map<String, String>> history = conversations.get(chatId);
        history.add(Map.of("role", "user", "content", text));

        String response;
        try {
            response = aiAgent.processMessage(history, chatId);
        } catch (Exception e) {
            log.error("Agent error for tg={}: {}", chatId, e.getMessage());
            response = "Произошла внутренняя ошибка. Попробуйте позже.";
        }

        history.add(Map.of("role", "assistant", "content", response));
        pruneHistory(chatId);

        try {
            SendMessage msg = new SendMessage(chatId.toString(), response);
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Bot {} failed to reply: {}", index, e.getMessage());
        }
    }

    private void handleCommand(Long chatId, String text) {
        String response;
        switch (text) {
            case "/start":
                response = "Привет! Я AI-ассистент CRM TryNeuro.\n"
                    + "Напиши свой вопрос — помогу с контактами, записями и услугами.\n"
                    + "Команды:\n"
                    + "/new — начать новый диалог\n"
                    + "/help — справка";
                break;
            case "/new":
                conversations.remove(chatId);
                response = "Диалог очищен. Задавай новый вопрос.";
                break;
            case "/help":
                response = "Я могу:\n"
                    + "• Искать и создавать клиентов\n"
                    + "• Записывать на услуги\n"
                    + "• Искать услуги и сотрудников\n"
                    + "• Показывать и отменять записи\n"
                    + "• Настраивать уведомления\n"
                    + "• Формировать отчёты\n\n"
                    + "Просто напиши, что нужно сделать.";
                break;
            default:
                response = "Неизвестная команда. Используй /help для списка команд.";
        }

        try {
            SendMessage msg = new SendMessage(chatId.toString(), response);
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Bot {} failed to reply command: {}", index, e.getMessage());
        }
    }

    private void pruneHistory(Long chatId) {
        List<Map<String, String>> history = conversations.get(chatId);
        if (history == null) return;
        if (history.size() > MAX_HISTORY_EXCHANGES * 2) {
            conversations.put(chatId, new ArrayList<>(
                history.subList(history.size() - MAX_HISTORY_EXCHANGES * 2, history.size())));
        }
    }

    public void clearHistory(long chatId) {
        conversations.remove(chatId);
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
