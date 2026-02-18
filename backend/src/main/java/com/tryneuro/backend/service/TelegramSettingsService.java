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
import java.util.Optional;

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
            return notificationClient.getQrStatus(internalSecret, tenantId);
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

    @Transactional
    public void disconnect(String tenantId) {
        try {
            notificationClient.disconnect(internalSecret, tenantId);
        } catch (Exception e) {
            log.warn("Microservice disconnect failed: {}", e.getMessage());
        }
        // Принудительно ставим статус в базе
        updateDatabaseStatus(tenantId, false);
        notifyFrontend(tenantId, "DISCONNECTED");
    }

    @Transactional
    public void updateStatus(String tenantId, String status) {
        // Всегда уведомляем фронтенд о текущем статусе (для спиннеров и QR)
        notifyFrontend(tenantId, status);

        // В базу пишем ТОЛЬКО финальные состояния
        if ("CONNECTED".equals(status)) {
            updateDatabaseStatus(tenantId, true);
        } else if ("DISCONNECTED".equals(status)) {
            updateDatabaseStatus(tenantId, false);
        }
        // Промежуточные статусы (INITIALIZING, WAITING_QR) базу НЕ трогают
    }

    private void updateDatabaseStatus(String tenantId, boolean active) {
        Optional<TelegramSettings> settingsOpt = telegramSettingsRepository.findById(tenantId);
        boolean wasActive = settingsOpt.map(TelegramSettings::isActive).orElse(false);

        if (wasActive != active || settingsOpt.isEmpty()) {
            log.info("💾 Database state sync for {}: {} -> {}", tenantId, wasActive, active);
            jdbcTemplate.update(
                "INSERT INTO telegram_settings (tenant_id, is_active, connected_at) " +
                "VALUES (?, ?, ?) ON CONFLICT (tenant_id) DO UPDATE SET is_active = ?, connected_at = ?",
                tenantId, active, LocalDateTime.now(), active, LocalDateTime.now()
            );
        }
    }

    private void notifyFrontend(String tenantId, String status) {
        try {
            messagingTemplate.convertAndSend("/topic/telegram/" + tenantId, Map.of(
                "status", status,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.warn("WebSocket notification failed: {}", e.getMessage());
        }
    }

    public TelegramSettings getSettings(String tenantId) {
        return telegramSettingsRepository.findById(tenantId).orElse(null);
    }
}
