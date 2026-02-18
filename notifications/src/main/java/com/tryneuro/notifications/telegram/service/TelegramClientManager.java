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
            try { client.send(new TdApi.LogOut(), res -> {}); } catch (Exception e) {}
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
            }
        } catch (Exception e) {} finally { tenantsToCleanup.remove(tenantId); }
    }

    private SimpleTelegramClient createNewClientInstance(String tenantId) {
        log.info("🚀 Starting session: {}", tenantId);
        Path sessionPath = Paths.get(properties.getSessionsPath()).resolve(tenantId);
        sessionPath.toFile().mkdirs();

        APIToken apiToken = new APIToken(properties.getApiId(), properties.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("db"));

        SimpleTelegramClientBuilder builder = clientFactory.builder(settings);
        final SimpleTelegramClient[] clientHolder = new SimpleTelegramClient[1];

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            TdApi.AuthorizationState state = update.authorizationState;
            if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
                pendingQrLinks.put(tenantId, qrState.link);
                syncStatusWithBackend(tenantId, "WAITING_QR");
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                log.info("🌟 Tenant {} connected!", tenantId);
                pendingQrLinks.remove(tenantId);
                floodWaitUntil.remove(tenantId);
                syncStatusWithBackend(tenantId, "CONNECTED");
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                activeClients.remove(tenantId);
                syncStatusWithBackend(tenantId, "DISCONNECTED");
            }
        });

        SimpleTelegramClient client = builder.build(AuthenticationSupplier.qrCode());
        clientHolder[0] = client;
        return client;
    }

    private void handleIncomingError(String tenantId, Throwable ex) {
        if (ex == null) return;
        String msg = ex.getMessage();
        if (msg.contains("420") || msg.contains("FLOOD_WAIT")) {
            String secondsOnly = msg.replaceAll("[^0-9]", "");
            int seconds = secondsOnly.isEmpty() ? 600 : Integer.parseInt(secondsOnly);
            floodWaitUntil.put(tenantId, System.currentTimeMillis() + (seconds * 1000));
            log.error("🛑 CRITICAL: Flood Wait during message chain for {}. Banned for {}s. Closing client.", tenantId, seconds);
            
            SimpleTelegramClient client = activeClients.remove(tenantId);
            if (client != null) {
                try { client.close(); } catch (Exception e) {}
            }
        }
    }

    public CompletableFuture<Void> sendMessageByPhone(String tenantId, String phoneNumber, String text) {
        SimpleTelegramClient client = getClient(tenantId);
        if (client == null) return CompletableFuture.failedFuture(new RuntimeException("TG client blocked or inactive"));

        TdApi.Contact contact = new TdApi.Contact();
        contact.phoneNumber = phoneNumber;
        contact.firstName = "Client";
        contact.lastName = "";

        return client.send(new TdApi.ImportContacts(new TdApi.Contact[]{contact}))
            .handle((imported, ex) -> {
                if (ex != null) { handleIncomingError(tenantId, ex); throw new RuntimeException(ex); }
                if (imported.userIds.length == 0 || imported.userIds[0] == 0) throw new RuntimeException("404: Not Found");
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
                log.info("✅ Message sent to {} via Contact Import", phoneNumber);
                return null;
            });
    }
}
