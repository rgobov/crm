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
    private final Map<String, Long> floodWaitUntil = new ConcurrentHashMap<>();
    
    private final AtomicBoolean isStopping = new AtomicBoolean(false);

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    // ПОРОГ БЕЗОПАСНОСТИ (Senior Logic):
    // Если Telegram просит ждать до 60 секунд (1 минута), мы считаем это штатной синхронизацией.
    // Если больше 60 секунд — это риск бана, закрываем сессию.
    private static final int CRITICAL_FLOOD_THRESHOLD_SEC = 60;

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
        if (isUnderFloodWait(tenantId)) return false;
        if (activeClients.containsKey(tenantId)) return true;
        
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId).resolve("db");
        if (new File(sessionPath.toString()).exists()) {
            getClient(tenantId); 
            return true; 
        }
        return false;
    }

    private boolean isUnderFloodWait(String tenantId) {
        Long banEndTime = floodWaitUntil.get(tenantId);
        if (banEndTime != null && banEndTime > System.currentTimeMillis()) {
            return true;
        }
        floodWaitUntil.remove(tenantId);
        return false;
    }

    public synchronized SimpleTelegramClient getClient(String tenantId) {
        if (isUnderFloodWait(tenantId) || tenantsToCleanup.contains(tenantId)) return null;
        if (activeClients.containsKey(tenantId)) return activeClients.get(tenantId);
        SimpleTelegramClient client = createNewClientInstance(tenantId);
        activeClients.put(tenantId, client);
        return client;
    }

    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized void deleteSession(String tenantId) {
        log.info("🗑 Deleting session for tenant: {}", tenantId);
        SimpleTelegramClient client = activeClients.remove(tenantId);
        if (client != null) {
            tenantsToCleanup.add(tenantId);
            try { client.send(new TdApi.LogOut(), res -> {
                log.info("🚪 LogOut command executed for tenant: {}", tenantId);
            }); } catch (Exception e) {}
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
                FileSystemUtils.deleteRecursively(directory);
                log.info("✨ Session files deleted for {}", tenantId);
            }
        } catch (Exception e) {
            log.error("❌ Cleanup failed: {}", e.getMessage());
        } finally { 
            tenantsToCleanup.remove(tenantId); 
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating new Telegram instance for: {}", tenantId);
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            log.info("🔄 AuthState change for {}: {}", tenantId, state.getClass().getSimpleName());
            
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                syncStatusWithBackend(tenantId, "WAITING_QR");
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 SUCCESS! Tenant {} is fully connected.", tenantId);
                pendingQrLinks.remove(tenantId);
                floodWaitUntil.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.warn("🔒 Connection closed for tenant {}", tenantId);
                activeClients.remove(tenantId);
                syncStatusWithBackend(tenantId, "DISCONNECTED");
            }
        });

        return builder.build(AuthenticationSupplier.qrCode());
    }

    private void handleIncomingError(String tenantId, Throwable ex) {
        if (ex == null) return;
        String msg = ex.getMessage();
        if (msg.contains("420") || msg.contains("FLOOD_WAIT")) {
            String secondsOnly = msg.replaceAll("[^0-9]", "");
            int seconds = secondsOnly.isEmpty() ? 600 : Integer.parseInt(secondsOnly);
            
            if (seconds > CRITICAL_FLOOD_THRESHOLD_SEC) {
                // КРИТИЧЕСКИЙ ФЛУД (> 1 мин): Закрываем сессию для безопасности аккаунта
                floodWaitUntil.put(tenantId, System.currentTimeMillis() + (seconds * 1000));
                log.error("🛑 RISK OF BAN: Critical Flood Wait ({}s) for {}. Closing session.", seconds, tenantId);
                SimpleTelegramClient client = activeClients.remove(tenantId);
                if (client != null) {
                    try { client.close(); } catch (Exception e) {}
                }
            } else {
                // ШТАТНОЕ ОЖИДАНИЕ (<= 1 мин): TDLib подождет сама, сессию не рвем
                log.warn("⏳ Transient Flood Wait ({}s) for {}. TDLib will auto-retry. Session kept ALIVE.", seconds, tenantId);
            }
        } else {
            log.error("⚠️ Incoming error for {}: {}", tenantId, msg);
        }
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        if (client == null) return CompletableFuture.failedFuture(new RuntimeException("TG client blocked or inactive"));

        log.info("📤 Sending Telegram message to {}", phoneNumber);
        TdApi.Contact contact = new TdApi.Contact();
        contact.phoneNumber = phoneNumber;
        contact.firstName = "Client";
        contact.lastName = "";

        return client.send(new TdApi.ImportContacts(new TdApi.Contact[]{contact}))
            .handle((imported, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) throw new RuntimeException("404: Phone not found");
                return imported.userIds[0];
            })
            .thenCompose(userId -> client.send(new TdApi.CreatePrivateChat(userId, false)))
            .handle((chat, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                return chat;
            })
            .thenCompose(chat -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText();
                content.text = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
                return client.send(new TdApi.SendMessage(chat.id, 0, null, null, null, content));
            })
            .handle((msg, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                log.info("✅ Success! Message sent to {} (Tenant: {})", phoneNumber, tenantId);
                return null;
            });
    }
}
