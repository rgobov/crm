package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.model.TelegramSettings;
import com.tryneuro.backend.repository.CompanyRepository;
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
    private final CompanyRepository companyRepository;
    private final NotificationClient notificationClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    public Map<String, String> getRealTimeStatus(String tenantId) {
        try {
            return notificationClient.getQrStatus(internalSecret, tenantId);
        } catch (Exception e) {
            log.error("Failed to get TG status from microservice: {}", e.getMessage());
            return Map.of("status", "ERROR", "qrCode", "");
        }
    }

    public void connect(String tenantId) {
        log.info("Initiating Telegram connect for tenant: {}", tenantId);
        try {
            notificationClient.connect(internalSecret, tenantId);
        } catch (Exception e) {
            log.error("Failed to call microservice connect: {}", e.getMessage());
        }
    }

    @Transactional
    public void disconnect(String tenantId) {
        log.info("Initiating Telegram disconnect for tenant: {}", tenantId);
        try {
            notificationClient.disconnect(internalSecret, tenantId);
        } catch (Exception e) {
            log.warn("Microservice disconnect failed: {}", e.getMessage());
        }

        telegramSettingsRepository.findById(tenantId).ifPresent(s -> {
            s.setActive(false);
            s.setConnectedPhone(null);
            telegramSettingsRepository.save(s);
        });

        notifyFrontend(tenantId, "DISCONNECTED");
    }

    @Transactional
    public void updateStatus(String tenantId, String status) {
        log.info("Updating Telegram status for tenant {}: {}", tenantId, status);
        
        if ("CONNECTED".equals(status)) {
            Optional<TelegramSettings> existing = telegramSettingsRepository.findById(tenantId);
            
            if (existing.isPresent()) {
                TelegramSettings settings = existing.get();
                settings.setActive(true);
                settings.setConnectedAt(LocalDateTime.now());
            } else {
                log.info("Initializing new Telegram settings for tenant {} via SQL", tenantId);
                jdbcTemplate.update(
                    "INSERT INTO telegram_settings (tenant_id, is_active, connected_at) VALUES (?, ?, ?)",
                    tenantId, true, LocalDateTime.now()
                );
            }
        } else if ("DISCONNECTED".equals(status)) {
            telegramSettingsRepository.findById(tenantId).ifPresent(s -> {
                s.setActive(false);
            });
        }

        notifyFrontend(tenantId, status);
    }

    private void notifyFrontend(String tenantId, String status) {
        try {
            messagingTemplate.convertAndSend("/topic/telegram/" + tenantId, Map.of(
                "status", status,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    public TelegramSettings getSettings(String tenantId) {
        return telegramSettingsRepository.findById(tenantId).orElse(null);
    }
}
