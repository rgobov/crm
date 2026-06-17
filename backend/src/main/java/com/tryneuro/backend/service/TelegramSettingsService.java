package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.model.TelegramSettings;
import com.tryneuro.backend.repository.TelegramSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramSettingsService {

    private final TelegramSettingsRepository telegramSettingsRepository;
    private final NotificationClient notificationClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    public Map<String, String> getRealTimeStatus(String tenantId) {
        try {
            return notificationClient.getStatus(internalSecret, tenantId);
        } catch (Exception e) {
            log.error("Failed to get TG status: {}", e.getMessage());
            return Map.of("status", "ERROR", "qrCode", "");
        }
    }

    public void connect(String tenantId) {
        try {
            notificationClient.connect(internalSecret, tenantId);
        } catch (Exception e) {
            log.error("Failed microservice connect call: {}", e.getMessage());
        }
    }

    public void checkPassword(String tenantId, String password) {
        try {
            notificationClient.checkPassword(internalSecret, Map.of(
                "tenantId", tenantId,
                "password", password
            ));
        } catch (Exception e) {
            log.error("Failed to proxy TG password: {}", e.getMessage());
        }
    }

    public Map<String, String> sendCode(String tenantId, String phoneNumber) {
        try {
            return notificationClient.sendCode(internalSecret, Map.of(
                "tenantId", tenantId,
                "phoneNumber", phoneNumber
            ));
        } catch (Exception e) {
            log.error("Failed to send code: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    public Map<String, String> signIn(String tenantId, String code) {
        try {
            return notificationClient.signIn(internalSecret, Map.of(
                "tenantId", tenantId,
                "code", code
            ));
        } catch (Exception e) {
            log.error("Failed to sign in: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    public void cancelQrGeneration(String tenantId) {
        try {
            notificationClient.cancelQrGeneration(internalSecret, tenantId);
            updateDatabaseStatus(tenantId, false);
        } catch (Exception e) {
            log.error("Failed to cancel QR generation: {}", e.getMessage());
        }
    }

    @Transactional
    public void disconnect(String tenantId) {
        try {
            notificationClient.disconnect(internalSecret, tenantId);
        } catch (Exception e) {
            log.warn("Microservice disconnect failed: {}", e.getMessage());
        }
        updateDatabaseStatus(tenantId, false);
        Map<String, Object> data = new HashMap<>();
        data.put("status", "DISCONNECTED");
        updateStatus(tenantId, data);
    }

    @Transactional
    public void updateStatus(String tenantId, Map<String, Object> data) {
        String status = (String) data.get("status");
        
        // ФИКС: Передаем всё содержимое (включая qrCode и ts) во фронтенд
        Map<String, Object> payload = new HashMap<>(data);
        payload.put("ts", System.currentTimeMillis());
        
        try {
            messagingTemplate.convertAndSend("/topic/telegram/" + tenantId, payload);
        } catch (Exception e) {
            log.warn("WebSocket notification failed: {}", e.getMessage());
        }

        if ("CONNECTED".equals(status)) {
            updateDatabaseStatus(tenantId, true);
        } else if ("DISCONNECTED".equals(status)) {
            updateDatabaseStatus(tenantId, false);
        }
    }

    private void updateDatabaseStatus(String tenantId, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO telegram_settings (tenant_id, is_active, connected_at) " +
            "VALUES (?, ?, ?) ON CONFLICT (tenant_id) DO UPDATE SET is_active = ?, connected_at = ?",
            tenantId, active, LocalDateTime.now(), active, LocalDateTime.now()
        );
    }

    public TelegramSettings getSettings(String tenantId) {
        return telegramSettingsRepository.findById(tenantId).orElse(null);
    }
}
