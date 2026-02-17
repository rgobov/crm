package com.tryneuro.notifications.telegram.service;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import com.tryneuro.notifications.telegram.config.TelegramProperties;
import com.tryneuro.notifications.telegram.client.BackendClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientManager {

    private final TelegramProperties properties;
    private final BackendClient backendClient;
    private final Map<String, SimpleTelegramClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> pendingQrLinks = new ConcurrentHashMap<>();
    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

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

    private void syncStatusWithBackend(String tenantId, String status) {
        try {
            backendClient.syncStatus(internalSecret, Map.of("tenantId", tenantId, "status", status));
            if (!"WAITING_QR".equals(status)) {
                log.info("📡 Sync status '{}' for tenant {}", status, tenantId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Sync failed for {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * ПОЛУЧЕНИЕ QR (без создания новой сессии)
     */
    public String getQrLink(String tenantId) {
        // Мы НЕ вызываем getClient здесь, чтобы не плодить сессии автоматически
        return pendingQrLinks.get(tenantId);
    }

    public boolean isSessionActive(String tenantId) {
        return activeClients.containsKey(tenantId);
    }

    public synchronized SimpleTelegramClient getClient(String tenantId) {
        if (activeClients.containsKey(tenantId)) {
            return activeClients.get(tenantId);
        }
        SimpleTelegramClient client = createNewClientInstance(tenantId);
        activeClients.put(tenantId, client);
        return client;
    }

    public synchronized void deleteSession(String tenantId) {
        log.info("🗑 Request to delete session for tenant: {}", tenantId);
        SimpleTelegramClient client = activeClients.remove(tenantId);
        
        if (client != null) {
            tenantsToCleanup.add(tenantId);
            pendingQrLinks.remove(tenantId);
            client.send(new TdApi.LogOut(), res -> {
                log.info("Logout signal sent for {}, waiting for Closed state", tenantId);
            });
        } else {
            cleanupFiles(tenantId);
            syncStatusWithBackend(tenantId, "DISCONNECTED");
        }
    }

    private void cleanupFiles(String tenantId) {
        try {
            Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
            File directory = sessionPath.toFile();
            if (directory.exists()) {
                Thread.sleep(1000);
                boolean deleted = FileSystemUtils.deleteRecursively(directory);
                log.info("📁 Physical cleanup for {}: {}", tenantId, deleted ? "SUCCESS" : "FAILED");
            }
        } catch (Exception e) {
            log.error("❌ Error during cleanup: {}", e.getMessage());
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating instance for tenant: {}", tenantId);

        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        new File(sessionPath.toString()).mkdirs();
        new File(sessionPath.resolve("db").toString()).mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));
        settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);
        final SimpleTelegramClient[] clientHolder = new SimpleTelegramClient[1];

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;

            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                syncStatusWithBackend(tenantId, "WAITING_QR");
            } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
                if (clientHolder[0] != null) {
                    clientHolder[0].send(new TdApi.RequestQrCodeAuthentication(), res -> {});
                }
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} connected!", tenantId);
                pendingQrLinks.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.info("📡 Session closed for tenant: {}", tenantId);
                activeClients.remove(tenantId);
                if (tenantsToCleanup.remove(tenantId)) {
                    cleanupFiles(tenantId);
                }
                syncStatusWithBackend(tenantId, "DISCONNECTED");
            }
        });

        SimpleTelegramClient client = builder.build(AuthenticationSupplier.qrCode());
        clientHolder[0] = client;
        return client;
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
            }).thenAccept(v -> {});
    }
}
