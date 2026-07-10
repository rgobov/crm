package com.tryneuro.aibot;

import com.tryneuro.aibot.service.AiAgentService;
import com.tryneuro.aibot.service.WhisperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TryNeuroBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TryNeuroBot.class);

    private final String token;
    private final String username;
    private final int index;
    private final AiAgentService aiAgent;
    private final WhisperService whisperService;

    private final Map<Long, List<Map<String, String>>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_EXCHANGES = 10;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public TryNeuroBot(String token, String username, int index, DefaultBotOptions options,
                       AiAgentService aiAgent, WhisperService whisperService) {
        super(options);
        this.token = token;
        this.username = username;
        this.index = index;
        this.aiAgent = aiAgent;
        this.whisperService = whisperService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Bot {} received update: hasMessage={}", index, update.hasMessage());

        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();
        String text;

        if (update.getMessage().hasVoice()) {
            Voice voice = update.getMessage().getVoice();
            log.info("Bot {} received voice from chat_id={}, duration={}s, mimeType={}", index, chatId,
                    voice.getDuration(), voice.getMimeType());
            text = transcribeVoice(chatId, voice);
            if (text == null || text.isBlank()) {
                sendReply(chatId, "Не удалось распознать голосовое сообщение. Напишите текстом.");
                return;
            }
        } else if (update.getMessage().hasText()) {
            text = update.getMessage().getText().strip();
        } else {
            return;
        }

        log.info("Bot {} processing from chat_id={}: \"{}\"", index, chatId, text);

        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            handleCommand(chatId, text);
            return;
        }

        ScheduledFuture<?> typingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                SendChatAction action = SendChatAction.builder()
                    .chatId(chatId.toString())
                    .action("typing")
                    .build();
                execute(action);
                log.debug("Bot {} typing sent to {}", index, chatId);
            } catch (TelegramApiException e) {
                log.warn("Bot {} failed to send typing: {}", index, e.getMessage());
            }
        }, 0, 4, TimeUnit.SECONDS);

        conversations.computeIfAbsent(chatId, k -> new ArrayList<>());
        List<Map<String, String>> history = conversations.get(chatId);
        history.add(Map.of("role", "user", "content", text));

        long startMs = System.currentTimeMillis();
        String response;
        try {
            response = aiAgent.processMessage(history, chatId);
        } catch (Exception e) {
            log.error("Agent error for tg={}: {}", chatId, e.getMessage(), e);
            response = "Произошла внутренняя ошибка. Попробуйте позже.";
        }
        long elapsed = System.currentTimeMillis() - startMs;

        typingTask.cancel(false);

        log.info("Bot {} agent responded in {}ms for chat_id={}, response len={}",
            index, elapsed, chatId, response != null ? response.length() : 0);
        log.debug("Bot {} response preview for {}: {}", index, chatId,
            response != null ? response.substring(0, Math.min(200, response.length())) : "null");

        history.add(Map.of("role", "assistant", "content", response));
        pruneHistory(chatId);

        try {
            SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(response)
                .build();
            execute(msg);
            log.info("Bot {} reply sent to {}", index, chatId);
        } catch (TelegramApiException e) {
            log.error("Bot {} failed to reply to {}: {}", index, chatId, e.getMessage(), e);
        }
    }

    private String transcribeVoice(Long chatId, Voice voice) {
        File oggFile = null;
        try {
            GetFile getFile = GetFile.builder().fileId(voice.getFileId()).build();
            org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
            oggFile = downloadFile(tgFile);
            log.info("Bot {} downloaded voice for chat_id={}: {} bytes", index, chatId, oggFile.length());
            return whisperService.transcribe(oggFile.toPath());
        } catch (Exception e) {
            log.error("Bot {} voice transcription failed for chat_id={}: {}", index, chatId, e.getMessage(), e);
            return null;
        } finally {
            if (oggFile != null && oggFile.exists()) {
                oggFile.delete();
            }
        }
    }

    private void sendReply(Long chatId, String text) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build();
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Bot {} failed to reply to {}: {}", index, chatId, e.getMessage());
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
