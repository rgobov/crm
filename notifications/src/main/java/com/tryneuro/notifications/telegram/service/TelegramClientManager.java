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
            warmupClients(); // ВОССТАНОВЛЕНО: Авто-запуск существующих сессий
        } catch (Exception e) {
            log.error("❌ Failed to initialize TDLib system", e);
        }
    }

    /**
     * Сканирует папку сессий и автоматически подключает всех клиентов
     */
    private void warmupClients() {
        try {
            File sessionsDir = new File(properties.getSessionsPath());
            if (sessionsDir.exists() && sessionsDir.isDirectory()) {
                File[] folders = sessionsDir.listFiles(File::isDirectory);
                if (folders != null) {
                    log.info("🚀 Starting auto-warmup for {} telegram clients...", folders.length);
                    for (File folder : folders) {
                        String tenantId = folder.getName();
                        log.info("💤 Waking up client for tenant: {}", tenantId);
                        // Запускаем в фоновом режиме, чтобы не тормозить старт приложения
                        CompletableFuture.runAsync(() -> getClient(tenantId));
                    }
                }
            }
        } catch (Exception e) {
            log.error("⚠️ Failed to warmup telegram clients", e);
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
            log.info("📡 Sync status '{}' for tenant {}", status, tenantId);
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
        
        return "DISCONNECTED";
    }

    public void initiateReconnect(String tenantId) {
        ReentrantLock lock = getSessionLock(tenantId);
        lock.lock();
        try {
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

    public void checkPassword(String tenantId, String password) {
        SimpleTelegramClient client = activeClients.get(tenantId);
        if (client != null) {
            log.info("🔑 Sending 2FA password for tenant: {}", tenantId);
            client.send(new TdApi.CheckAuthenticationPassword(password), res -> {
                if (res.isError()) {
                    log.error("❌ 2FA Password incorrect for {}: {}", tenantId, res.getError().message);
                    syncStatusWithBackend(tenantId, "PASSWORD_ERROR");
                } else {
                    log.info("✅ 2FA Password accepted for {}", tenantId);
                }
            });
        }
    }

    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void forceDisconnect(String tenantId) {
        ReentrantLock lock = getSessionLock(tenantId);
        lock.lock();
        try {
            tenantsToCleanup.add(tenantId);
            pendingQrLinks.remove(tenantId);
            lastSyncedQrLink.remove(tenantId);

            SimpleTelegramClient client = activeClients.remove(tenantId);
            if (client != null) {
                CompletableFuture<Void> closeFut = new CompletableFuture<>();
                pendingCloses.put(tenantId, closeFut);
                try {
                    client.close();
                    closeFut.get(10, TimeUnit.SECONDS); 
                } catch (Exception e) {
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
        } catch (Exception e) {}
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                String newLink = qrState.link;
                pendingQrLinks.put(tenantId, newLink);
                if (!newLink.equals(lastSyncedQrLink.get(tenantId))) {
                    lastSyncedQrLink.put(tenantId, newLink);
                    syncStatusWithBackend(tenantId, "WAITING_QR");
                }
            } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
                log.warn("🔐 2FA Password required for {}", tenantId);
                pendingQrLinks.remove(tenantId);
                syncStatusWithBackend(tenantId, "WAITING_PASSWORD");
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                pendingQrLinks.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                activeClients.remove(tenantId);
                pendingQrLinks.remove(tenantId);
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
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) {
                    CompletableFuture<TdApi.Chat> fail = new CompletableFuture<>();
                    fail.completeExceptionally(new RuntimeException("404"));
                    return fail;
                }
                return client.send(new TdApi.CreatePrivateChat(imported.userIds[0], false));
            })
            .thenCompose(chat -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText();
                content.text = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
                return client.send(new TdApi.SendMessage(chat.id, 0, null, null, null, content));
            })
            .thenAccept(msg -> {
                log.info("✅ Sent to {}", phoneNumber);
            });
    }
}
