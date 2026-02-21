package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.TelegramSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManager {

    private final TelegramSettingsService telegramSettingsService;
    private final NotificationTemplateService templateService;
    private final TemplateEngineService templateEngine;
    private final NotificationClient notificationClient;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    public void sendNotification(Appointment appointment, String type) {
        String tenantId = appointment.getTenantId();
        log.info("🔔 [MANAGER] Notification request received. AppID: {}, Type: {}, Tenant: {}", appointment.getId(), type, tenantId);
        
        if (!appointment.isAllowReminder()) {
            log.info("🔕 [MANAGER] Reminders are DISABLED for appointment: {}", appointment.getId());
            return;
        }

        TelegramSettings tg = telegramSettingsService.getSettings(tenantId);
        if (tg != null && tg.isActive()) {
            log.info("📤 [MANAGER] Found ACTIVE Telegram provider for tenant {}. Starting sendViaTelegram...", tenantId);
            sendViaTelegram(appointment, type);
            return;
        }

        log.warn("⚠️ [MANAGER] No active notification provider found for tenant: {}. Check telegram_settings in DB.", tenantId);
    }

    private void sendViaTelegram(Appointment appointment, String type) {
        String tenantId = appointment.getTenantId();
        String template = templateService.getTemplateContent(tenantId, type);
        String message = templateEngine.process(template, appointment);

        log.info("🔍 [TELEGRAM] Checking contact data for app {}. Contact object: {}", 
                appointment.getId(), appointment.getContact() != null ? "FOUND" : "NULL");

        if (appointment.getContact() != null && 
            appointment.getContact().getPhones() != null && 
            !appointment.getContact().getPhones().isEmpty()) {
            
            String rawPhone = appointment.getContact().getPhones().get(0);
            log.info("📱 [TELEGRAM] Phone from DB: {}", rawPhone);
            
            // 1. Очищаем от всего, кроме цифр
            String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
            
            // 2. Умная коррекция для РФ (если 11 цифр и начинается с 8)
            if (cleanPhone.length() == 11 && cleanPhone.startsWith("8")) {
                cleanPhone = "7" + cleanPhone.substring(1);
            }
            
            log.info("🚀 [TELEGRAM] Dispatching message to microservice. Phone: {}, Tenant: {}", cleanPhone, tenantId);
            
            try {
                Map<String, String> response = notificationClient.sendTelegramMessage(internalSecret, Map.of(
                    "tenantId", tenantId,
                    "phone", cleanPhone,
                    "text", message
                ));
                log.info("✅ [TELEGRAM] SUCCESS! Microservice response: {}", response);
            } catch (Exception e) {
                log.error("❌ [TELEGRAM] FAILED to send to microservice for {}: {}", cleanPhone, e.getMessage());
                throw e;
            }
        } else {
            log.error("❌ [TELEGRAM] ABORT: No phone number found for contact in appointment {}", appointment.getId());
        }
    }
}
