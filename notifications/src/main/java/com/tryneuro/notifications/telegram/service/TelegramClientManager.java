package com.tryneuro.notifications.telegram.service;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import com.tryneuro.notifications.telegram.config.TelegramProperties;
import com.tryneuro.notifications.telegram.client.BackendClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientManager {

    private final TelegramProperties properties;
    private final BackendClient backendClient;
    private final Map<String, SimpleTelegramClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> pendingQrLinks = new ConcurrentHashMap<>();
    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, AtomicBoolean> qrRequestedFlags = new ConcurrentHashMap<>();
    private final Map<String, Long> qrStartTime = new ConcurrentHashMap<>(); // Для отслеживания таймаута
    
    private final AtomicBoolean isStopping = new AtomicBoolean(false);

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    @PostConstruct
    public void init() {
        try {
            Init.init();
            log.info("✅ TDLib system initialized.");
        } catch (Exception e) {
            log.error("❌ Failed to initialize TDLib system", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        isStopping.set(true);
        activeClients.values().forEach(client -> {
            try { client.close(); } catch (Exception e) {}
        });
    }

    private void syncStatusWithBackend(String tenantId, String status) {
        if (isStopping.get()) return;
        try {
            backendClient.syncStatus(internalSecret, Map.of("tenantId", tenantId, "status", status));
            // НЕ ЛОГИРУЕМ WAITING_QR, чтобы не спамить в консоль
            if (!"WAITING_QR".equals(status)) {
                log.info("📡 Sync status '{}' for tenant {}", status, tenantId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Sync failed for {}: {}", tenantId, e.getMessage());
        }
    }

    public String getQrLink(String tenantId) {
        return pendingQrLinks.get(tenantId);
    }

    public boolean isSessionActive(String tenantId) {
        if (activeClients.containsKey(tenantId)) return true;
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId).resolve("db");
        if (new File(sessionPath.toString()).exists()) {
            getClient(tenantId); 
            return true; 
        }
        return false;
    }

    public synchronized SimpleTelegramClient getClient(String tenantId) {
        if (tenantsToCleanup.contains(tenantId)) return null;
        if (activeClients.containsKey(tenantId)) {
            qrRequestedFlags.remove(tenantId);
            return activeClients.get(tenantId);
        }
        
        SimpleTelegramClient client = createNewClientInstance(tenantId);
        activeClients.put(tenantId, client);
        return client;
    }

    public synchronized void deleteSession(String tenantId) {
        log.info("🗑 Request to delete session for tenant: {}", tenantId);
        pendingQrLinks.remove(tenantId);
        qrRequestedFlags.remove(tenantId);
        qrStartTime.remove(tenantId);
        
        SimpleTelegramClient client = activeClients.remove(tenantId);
        if (client != null) {
            tenantsToCleanup.add(tenantId);
            try {
                client.send(new TdApi.LogOut(), res -> {});
            } catch (Exception e) {
                tenantsToCleanup.remove(tenantId);
            }
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
                Thread.sleep(1500);
                FileSystemUtils.deleteRecursively(directory);
                log.info("📁 Physical cleanup for {}: SUCCESS", tenantId);
            }
        } catch (Exception e) {
            log.error("❌ Error during cleanup: {}", e.getMessage());
        } finally {
            tenantsToCleanup.remove(tenantId);
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating session for tenant: {}", tenantId);

        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        new File(sessionPath.toString()).mkdirs();
        new File(sessionPath.resolve("db").toString()).mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));
        settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));
        settings.setUseTestDatacenter(false);

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);
        final SimpleTelegramClient[] clientHolder = new SimpleTelegramClient[1];

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;

            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                
                // ТАЙМАУТ: Если QR висит больше 5 минут - закрываем сессию
                long startTime = qrStartTime.computeIfAbsent(tenantId, k -> System.currentTimeMillis());
                if (System.currentTimeMillis() - startTime > 300000) {
                    log.warn("⏱ QR Timeout for tenant {}. Aborting session.", tenantId);
                    deleteSession(tenantId);
                    return;
                }
                
                syncStatusWithBackend(tenantId, "WAITING_QR");
                
            } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
                AtomicBoolean alreadyRequested = qrRequestedFlags.computeIfAbsent(tenantId, k -> new AtomicBoolean(false));
                if (clientHolder[0] != null && alreadyRequested.compareAndSet(false, true)) {
                    log.info("📸 Requesting initial QR for tenant: {}", tenantId);
                    clientHolder[0].send(new TdApi.RequestQrCodeAuthentication(), res -> {
                        if (res.isError()) alreadyRequested.set(false); 
                    });
                }
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} connected!", tenantId);
                pendingQrLinks.remove(tenantId);
                qrRequestedFlags.remove(tenantId);
                qrStartTime.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.info("📡 Session closed for tenant: {}", tenantId);
                activeClients.remove(tenantId);
                qrRequestedFlags.remove(tenantId);
                qrStartTime.remove(tenantId);
                if (!isStopping.get()) {
                    if (tenantsToCleanup.contains(tenantId)) cleanupFiles(tenantId);
                    syncStatusWithBackend(tenantId, "DISCONNECTED");
                }
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
