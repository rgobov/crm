package com.tryneuro.notifications.telegram.service;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import it.tdlight.tdlight.ClientManager;
import com.tryneuro.notifications.telegram.config.TelegramProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientManager {

    private final TelegramProperties properties;
    private final ClientManager clientManager = ClientManager.create();
    private final Map<String, SimpleTelegramClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> pendingQrLinks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Init.init();
            log.info("✅ TDLib system initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize TDLib system", e);
        }
    }

    public String getQrLink(String tenantId) {
        getClient(tenantId);
        return pendingQrLinks.get(tenantId);
    }

    public SimpleTelegramClient getClient(String tenantId) {
        return activeClients.computeIfAbsent(tenantId, this::createNewClientInstance);
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating Telegram session instance for tenant: {}", tenantId);

        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        
        TDLibSettings settings = TDLibSettings.create(properties.getApiId(), properties.getApiHash());
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));
        settings.setFilesDirectoryPath(sessionPath.resolve("files"));

        SimpleTelegramClientBuilder builder = SimpleTelegramClient.builder();
        builder.setTDLibSettings(settings);
        builder.setClientManager(clientManager);

        builder.setAuthenticationSupplier(new AuthenticationSupplier<>() {
            @Override
            public CompletableFuture<Void> onShortQrCode(String link) {
                log.info("📸 NEW QR LINK for {}: {}", tenantId, link);
                pendingQrLinks.put(tenantId, link);
                return CompletableFuture.completedFuture(null);
            }
            @Override public CompletableFuture<TdApi.AuthenticationServerProtocol> onAuthenticationServerProtocol() { return null; }
            @Override public CompletableFuture<String> onBotToken() { return null; }
            @Override public CompletableFuture<Long> onParameter() { return null; }
            @Override public CompletableFuture<String> onPassword(String hint) { return null; }
            @Override public CompletableFuture<String> onPhoneNumber() { return null; }
            @Override public CompletableFuture<String> onEmailAddress() { return null; }
            @Override public CompletableFuture<String> onEmailCode() { return null; }
            @Override public CompletableFuture<String> onCode() { return null; }
        });

        SimpleTelegramClient client = builder.build();

        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            if (update.authorizationState instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} is now AUTHORIZED and READY!", tenantId);
                pendingQrLinks.remove(tenantId);
            }
        });

        return client;
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        CompletableFuture<Void> result = new CompletableFuture<>();

        TdApi.SearchUserByPhoneNumber search = new TdApi.SearchUserByPhoneNumber();
        search.phoneNumber = phoneNumber;

        client.send(search).thenAccept(user -> {
            TdApi.CreatePrivateChat createChat = new TdApi.CreatePrivateChat();
            createChat.userId = user.id;
            createChat.force = false;

            client.send(createChat).thenAccept(chat -> {
                TdApi.SendMessage sendMsg = new TdApi.SendMessage();
                sendMsg.chatId = chat.id;
                sendMsg.inputMessageContent = new TdApi.InputMessageText(
                    new TdApi.FormattedText(text, null), false, true
                );

                client.send(sendMsg).thenAccept(msg -> {
                    log.info("📤 Message successfully sent to {} (Tenant: {})", phoneNumber, tenantId);
                    result.complete(null);
                }).exceptionally(ex -> { result.completeExceptionally(ex); return null; });
            });
        }).exceptionally(ex -> {
            log.warn("❌ User not found by phone: {}", phoneNumber);
            result.completeExceptionally(ex);
            return null;
        });

        return result;
    }
}
