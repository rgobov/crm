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

    private static final int CRITICAL_FLOOD_THRESHOLD_SEC = 600;

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

    // НОВОЕ: Возвращаем не просто true/false, а детальный статус
    public String getExtendedStatus(String tenantId) {
        Long waitTime = floodWaitUntil.get(tenantId);
        if (waitTime != null && waitTime > System.currentTimeMillis()) {
            return "FLOOD_WAIT_" + ((waitTime - System.currentTimeMillis()) / 1000);
        }
        
        if (activeClients.containsKey(tenantId)) return "CONNECTED";
        
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId).resolve("db");
        if (new File(sessionPath.toString()).exists()) {
            // Если файл есть, но клиент не в памяти - пробуем поднять "тихо"
            getClient(tenantId);
            return "INITIALIZING";
        }
        
        return "DISCONNECTED";
    }

    public boolean isSessionActive(String tenantId) {
        String status = getExtendedStatus(tenantId);
        return status.equals("CONNECTED") || status.startsWith("FLOOD_WAIT");
    }

    public synchronized SimpleTelegramClient getClient(String tenantId) {
        // Если жесткий бан (больше лимита), не даем клиент
        Long waitTime = floodWaitUntil.get(tenantId);
        if (waitTime != null && waitTime > System.currentTimeMillis()) {
            long diff = (waitTime - System.currentTimeMillis()) / 1000;
            if (diff > CRITICAL_FLOOD_THRESHOLD_SEC) return null;
        }
        
        if (tenantsToCleanup.contains(tenantId)) return null;
        if (activeClients.containsKey(tenantId)) return activeClients.get(tenantId);
        
        SimpleTelegramClient client = createNewClientInstance(tenantId);
        activeClients.put(tenantId, client);
        return client;
    }

    private final Set<String> tenantsToCleanup = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized void deleteSession(String tenantId) {
        log.info("🗑 Deleting session for tenant: {}", tenantId);
        SimpleTelegramClient client = activeClients.remove(tenantId);
        floodWaitUntil.remove(tenantId); // Очищаем и бан тоже
        if (client != null) {
            tenantsToCleanup.add(tenantId);
            try { client.send(new TdApi.LogOut(), res -> {
                log.info("🚪 LogOut success for {}", tenantId);
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
                log.info("✨ Files cleared for {}", tenantId);
            }
        } catch (Exception e) {
            log.error("❌ Cleanup error: {}", e.getMessage());
        } finally { 
            tenantsToCleanup.remove(tenantId); 
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Creating Telegram instance: {}", tenantId);
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            log.info("🔄 AuthState change [{}]: {}", tenantId, state.getClass().getSimpleName());
            
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                syncStatusWithBackend(tenantId, "WAITING_QR");
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 SUCCESS! Tenant {} fully connected.", tenantId);
                pendingQrLinks.remove(tenantId);
                floodWaitUntil.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.warn("🔒 Connection CLOSED for {}", tenantId);
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
            
            floodWaitUntil.put(tenantId, System.currentTimeMillis() + (seconds * 1000));

            if (seconds > CRITICAL_FLOOD_THRESHOLD_SEC) {
                log.error("🛑 CRITICAL FLOOD ({}s). Protection disconnect for {}", seconds, tenantId);
                SimpleTelegramClient client = activeClients.remove(tenantId);
                if (client != null) { try { client.close(); } catch (Exception e) {} }
            } else {
                log.warn("⏳ Telegram request: Wait {}s for {}. TDLib is handling this...", seconds, tenantId);
            }
        } else {
            log.error("⚠️ Incoming error [{}]: {}", tenantId, msg);
        }
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        if (client == null) {
            String status = getExtendedStatus(tenantId);
            return CompletableFuture.failedFuture(new RuntimeException("TG_OFFLINE_" + status));
        }

        TdApi.Contact contact = new TdApi.Contact();
        contact.phoneNumber = phoneNumber;
        contact.firstName = "Client";

        return client.send(new TdApi.ImportContacts(new TdApi.Contact[]{contact}))
            .handle((imported, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) throw new RuntimeException("404");
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
                log.info("✅ Sent to {} ({})", phoneNumber, tenantId);
                return null;
            });
    }
}
