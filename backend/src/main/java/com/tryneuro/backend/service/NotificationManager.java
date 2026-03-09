package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.TelegramSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

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

        // ИСПРАВЛЕНО: Берем имя напрямую из поля clientName записи, это надежнее чем LAZY contact
        String clientName = appointment.getClientName();
        String rawPhone = appointment.getClientPhone();

        // Fallback если телефон не в записи, а в контакте (для старых записей)
        if ((rawPhone == null || rawPhone.isEmpty()) && appointment.getContact() != null) {
            rawPhone = (appointment.getContact().getPhones() != null && !appointment.getContact().getPhones().isEmpty()) 
                       ? appointment.getContact().getPhones().get(0) : null;
        }

        if (rawPhone != null && !rawPhone.isEmpty()) {
            String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 11 && cleanPhone.startsWith("8")) {
                cleanPhone = "7" + cleanPhone.substring(1);
            }
            
            log.info("🚀 [TELEGRAM] Dispatching message for {} to microservice. Phone: {}", clientName, cleanPhone);
            
            try {
                Map<String, String> requestData = new HashMap<>();
                requestData.put("tenantId", tenantId);
                requestData.put("phone", cleanPhone);
                requestData.put("name", clientName);
                requestData.put("text", message);

                notificationClient.sendTelegramMessage(internalSecret, requestData);
                log.info("✅ [TELEGRAM] SUCCESS for {} ({})", clientName, cleanPhone);
            } catch (Exception e) {
                log.error("❌ [TELEGRAM] FAILED to send to microservice for {}: {}", cleanPhone, e.getMessage());
                throw e;
            }
        } else {
            log.error("❌ [TELEGRAM] ABORT: No phone number found for appointment {}", appointment.getId());
        }
    }
}
