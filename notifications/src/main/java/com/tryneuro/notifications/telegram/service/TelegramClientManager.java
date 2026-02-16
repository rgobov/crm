package com.tryneuro.notifications.telegram.service;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import com.tryneuro.notifications.telegram.config.TelegramProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
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
    private final Map<String, SimpleTelegramClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> pendingQrLinks = new ConcurrentHashMap<>();

    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

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

    public synchronized SimpleTelegramClient getClient(String tenantId) {
        if (activeClients.containsKey(tenantId)) {
            return activeClients.get(tenantId);
        }
        SimpleTelegramClient client = createNewClientInstance(tenantId);
        activeClients.put(tenantId, client);
        return client;
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating session for tenant: {}", tenantId);

        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        File dbDir = sessionPath.resolve("db").toFile();
        if (!dbDir.exists()) dbDir.mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));
        settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;

            if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
                getClient(tenantId).send(new TdApi.RequestQrCodeAuthentication(), res -> {
                    if (res.isError()) log.error("QR Error for {}: {}", tenantId, res.getError().message);
                });
            } else if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                log.info("📸 NEW QR LINK for {}: {}", tenantId, qrState.link);
                pendingQrLinks.put(tenantId, qrState.link);
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} connected!", tenantId);
                pendingQrLinks.remove(tenantId);
            }
        });

        return builder.build(AuthenticationSupplier.consoleLogin());
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);

        TdApi.SearchUserByPhoneNumber searchRequest = new TdApi.SearchUserByPhoneNumber();
        searchRequest.phoneNumber = phoneNumber;

        return client.send(searchRequest)
            .thenCompose(user -> {
                TdApi.CreatePrivateChat createChatRequest = new TdApi.CreatePrivateChat();
                createChatRequest.userId = user.id;
                createChatRequest.force = false;
                return client.send(createChatRequest);
            })
            .thenCompose(chat -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText();
                content.text = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);

                TdApi.SendMessage sendMsg = new TdApi.SendMessage();
                sendMsg.chatId = chat.id;
                sendMsg.inputMessageContent = content;

                return client.send(sendMsg);
            })
            .thenAccept(msg -> log.info("📤 Message successfully sent to {}", phoneNumber))
            .exceptionally(ex -> {
                log.error("❌ Failed to send message to {}: {}", phoneNumber, ex.getMessage());
                throw new RuntimeException(ex);
            });
    }
}
