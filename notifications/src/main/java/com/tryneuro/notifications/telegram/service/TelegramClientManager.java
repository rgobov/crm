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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientManager {

    private final TelegramProperties properties;
    private final BackendClient backendClient;
    private final Map<String, SimpleTelegramClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> pendingQrLinks = new ConcurrentHashMap<>();
    private final Map<String, Long> floodWaitUntil = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> pendingCloses = new ConcurrentHashMap<>();
    // НОВОЕ: Для дедупликации спама WAITING_QR
    private final Map<String, String> lastSyncedQrLink = new ConcurrentHashMap<>();
    
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

    private ReentrantLock getSessionLock(String tenantId) {
        return sessionLocks.computeIfAbsent(tenantId, k -> new ReentrantLock());
    }

    private void syncStatusWithBackend(String tenantId, String status) {
        if (isStopping.get()) return;
        try {
            backendClient.syncStatus(internalSecret, Map.of("tenantId", tenantId, "status", status));
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

    public String getExtendedStatus(String tenantId) {
        if (tenantsToCleanup.contains(tenantId)) return "DISCONNECTING";
        
        Long waitTime = floodWaitUntil.get(tenantId);
        if (waitTime != null && waitTime > System.currentTimeMillis()) {
            return "FLOOD_WAIT_" + ((waitTime - System.currentTimeMillis()) / 1000);
        }

        if (pendingQrLinks.containsKey(tenantId)) {
            return "WAITING_QR";
        }
        
        if (activeClients.containsKey(tenantId)) return "CONNECTED";
        
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId).resolve("db");
        if (new File(sessionPath.toString()).exists()) {
            return "INITIALIZING";
        }
        
        return "DISCONNECTED";
    }

    public boolean isSessionActive(String tenantId) {
        String status = getExtendedStatus(tenantId);
        return status.equals("CONNECTED") || status.startsWith("FLOOD_WAIT");
    }

    public void initiateReconnect(String tenantId) {
        ReentrantLock lock = getSessionLock(tenantId);
        lock.lock();
        try {
            log.info("🔄 Atomic Reconnect starting for {}", tenantId);
            forceDisconnect(tenantId);
            getClient(tenantId);
        } finally {
            lock.unlock();
        }
    }

    public SimpleTelegramClient getClient(String tenantId) {
        ReentrantLock lock = getSessionLock(tenantId);
        lock.lock();
        try {
            if (tenantsToCleanup.contains(tenantId)) return null;
            if (activeClients.containsKey(tenantId)) return activeClients.get(tenantId);
            
            SimpleTelegramClient client = createNewClientInstance(tenantId);
            activeClients.put(tenantId, client);
            return client;
        } finally {
            lock.unlock();
        }
    }

    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void forceDisconnect(String tenantId) {
        ReentrantLock lock = getSessionLock(tenantId);
        lock.lock();
        try {
            log.info("🗑 Forced atomic disconnect for {}", tenantId);
            tenantsToCleanup.add(tenantId);
            pendingQrLinks.remove(tenantId);
            floodWaitUntil.remove(tenantId);
            lastSyncedQrLink.remove(tenantId); // НОВОЕ: Очистка кэша синхронизации

            SimpleTelegramClient client = activeClients.remove(tenantId);
            if (client != null) {
                CompletableFuture<Void> closeFut = new CompletableFuture<>();
                pendingCloses.put(tenantId, closeFut);
                try {
                    client.close();
                    closeFut.get(10, TimeUnit.SECONDS); 
                } catch (Exception e) {
                    log.warn("Close await timeout for {}: {}", tenantId, e.getMessage());
                    pendingCloses.remove(tenantId);
                }
            }

            cleanupFiles(tenantId);
            syncStatusWithBackend(tenantId, "DISCONNECTED");
        } finally {
            tenantsToCleanup.remove(tenantId);
            lock.unlock();
        }
    }

    private void cleanupFiles(String tenantId) {
        try {
            Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
            File directory = sessionPath.toFile();
            if (directory.exists()) {
                Thread.sleep(200); 
                FileSystemUtils.deleteRecursively(directory);
                log.info("✨ Files cleared for {}", tenantId);
            }
        } catch (Exception e) {
            log.error("❌ Cleanup error: {}", e.getMessage());
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating Telegram instance: {}", tenantId);
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        pendingQrLinks.put(tenantId, "");

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            log.info("🔄 AuthState change [{}]: {}", tenantId, state.getClass().getSimpleName());
            
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                String newLink = qrState.link;
                pendingQrLinks.put(tenantId, newLink);

                // НОВОЕ: Дедупликация - шлем WS только если ссылка реально изменилась
                if (!newLink.equals(lastSyncedQrLink.get(tenantId))) {
                    lastSyncedQrLink.put(tenantId, newLink);
                    syncStatusWithBackend(tenantId, "WAITING_QR");
                }
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 SUCCESS! Tenant {} connected.", tenantId);
                pendingQrLinks.remove(tenantId);
                floodWaitUntil.remove(tenantId);
                lastSyncedQrLink.remove(tenantId); // Очистка
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.warn("🔒 Connection CLOSED for {}", tenantId);
                activeClients.remove(tenantId);
                pendingQrLinks.remove(tenantId);
                lastSyncedQrLink.remove(tenantId); // Очистка

                CompletableFuture<Void> fut = pendingCloses.remove(tenantId);
                if (fut != null) fut.complete(null);
                syncStatusWithBackend(tenantId, "DISCONNECTED");
            }
        });

        return builder.build(AuthenticationSupplier.qrCode());
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        if (client == null) return CompletableFuture.failedFuture(new RuntimeException("OFFLINE"));

        TdApi.Contact contact = new TdApi.Contact();
        contact.phoneNumber = phoneNumber;
        contact.firstName = "Client";

        return client.send(new TdApi.ImportContacts(new TdApi.Contact[]{contact}))
            .thenCompose(imported -> {
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) throw new RuntimeException("404");
                return client.send(new TdApi.CreatePrivateChat(imported.userIds[0], false));
            })
            .thenCompose(chat -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText();
                content.text = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
                return client.send(new TdApi.SendMessage(chat.id, 0, null, null, null, content));
            })
            .thenAccept(msg -> log.info("✅ Sent to {}", phoneNumber));
    }
}
