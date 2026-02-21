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
    private final Map<String, AtomicBoolean> qrRequestedFlags = new ConcurrentHashMap<>();
    private final Map<String, Long> qrStartTime = new ConcurrentHashMap<>();
    
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
        log.info("🔍 Checking session file at: {}", sessionPath.toAbsolutePath());
        
        if (new File(sessionPath.toString()).exists()) {
            log.info("📁 Session file EXISTS for tenant: {}", tenantId);
            getClient(tenantId); 
            return true; 
        }
        log.warn("❌ Session file NOT FOUND for tenant: {}", tenantId);
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
                log.info("🚪 LogOut command sent for tenant: {}", tenantId);
            }); } catch (Exception e) {}
        } else {
            cleanupFiles(tenantId);
            syncStatusWithBackend(tenantId, "DISCONNECTED");
        }
    }

    private void cleanupFiles(String tenantId) {
        try {
            Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
            log.info("🧹 Cleaning up session files at: {}", sessionPath.toAbsolutePath());
            File directory = sessionPath.toFile();
            if (directory.exists()) {
                Thread.sleep(1000);
                FileSystemUtils.deleteRecursively(directory);
                log.info("✨ Files deleted successfully for {}", tenantId);
            }
        } catch (Exception e) {
            log.error("❌ Failed to cleanup files for {}: {}", tenantId, e.getMessage());
        } finally { 
            tenantsToCleanup.remove(tenantId); 
        }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Starting session instance for: {}", tenantId);
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        Path dbPath = sessionPath.resolve("db");
        log.info("⚙️ Initializing TDLib settings with path: {}", dbPath.toAbsolutePath());
        settings.setDatabaseDirectoryPath(dbPath);

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            log.info("🔄 AuthState Change for {}: {}", tenantId, state.getClass().getSimpleName());
            
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                syncStatusWithBackend(tenantId, "WAITING_QR");
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} connected successfully!", tenantId);
                pendingQrLinks.remove(tenantId);
                floodWaitUntil.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                log.warn("🔒 Session CLOSED for tenant {}", tenantId);
                activeClients.remove(tenantId);
                syncStatusWithBackend(tenantId, "DISCONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateLoggingOut) {
                log.info("🚪 Logging out for tenant {}", tenantId);
            } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
                log.info("📩 Waiting for SMS code for tenant {}", tenantId);
            } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
                log.info("🔑 Waiting for 2FA password for tenant {}", tenantId);
            }
        });

        SimpleTelegramClient client = builder.build(AuthenticationSupplier.qrCode());
        return client;
    }

    private void handleIncomingError(String tenantId, Throwable ex) {
        if (ex == null) return;
        String msg = ex.getMessage();
        log.error("⚠️ Incoming error for {}: {}", tenantId, msg);
        if (msg.contains("420") || msg.contains("FLOOD_WAIT")) {
            String secondsOnly = msg.replaceAll("[^0-9]", "");
            int seconds = secondsOnly.isEmpty() ? 600 : Integer.parseInt(secondsOnly);
            floodWaitUntil.put(tenantId, System.currentTimeMillis() + (seconds * 1000));
            log.error("🛑 CRITICAL: Flood Wait for {}. Banned for {}s.", tenantId, seconds);
            
            SimpleTelegramClient client = activeClients.remove(tenantId);
            if (client != null) {
                try { client.close(); } catch (Exception e) {}
            }
        }
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        if (client == null) return CompletableFuture.failedFuture(new RuntimeException("TG client blocked or inactive"));

        log.info("📤 Attempting to send message to {} (tenant: {})", phoneNumber, tenantId);
        TdApi.Contact contact = new TdApi.Contact();
        contact.phoneNumber = phoneNumber;
        contact.firstName = "Client";
        contact.lastName = "";

        return client.send(new TdApi.ImportContacts(new TdApi.Contact[]{contact}))
            .handle((imported, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) {
                    log.warn("❌ Phone {} not found in Telegram", phoneNumber);
                    throw new RuntimeException("404: Not Found");
                }
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
                log.info("✅ Success! Message sent to {} for tenant {}", phoneNumber, tenantId);
                return null;
            });
    }
}
