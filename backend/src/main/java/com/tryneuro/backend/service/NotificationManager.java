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
        
        if (!appointment.isAllowReminder()) {
            log.info("Notifications disabled for appointment: {}", appointment.getId());
            return;
        }

        TelegramSettings tg = telegramSettingsService.getSettings(tenantId);
        if (tg != null && tg.isActive()) {
            sendViaTelegram(appointment, type);
            return;
        }

        log.warn("No active notification provider found for tenant: {}", tenantId);
    }

    private void sendViaTelegram(Appointment appointment, String type) {
        String tenantId = appointment.getTenantId();
        String template = templateService.getTemplateContent(tenantId, type);
        String message = templateEngine.process(template, appointment);

        if (appointment.getContact() != null && 
            appointment.getContact().getPhones() != null && 
            !appointment.getContact().getPhones().isEmpty()) {
            
            String rawPhone = appointment.getContact().getPhones().get(0);
            
            // 1. Очищаем от всего, кроме цифр
            String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
            
            // 2. Умная коррекция для РФ (если 11 цифр и начинается с 8)
            if (cleanPhone.length() == 11 && cleanPhone.startsWith("8")) {
                cleanPhone = "7" + cleanPhone.substring(1);
            }
            
            // 3. Если номер все еще начинается с 8 (и длина не 11), 
            // мы его не трогаем, так как это может быть международный номер другой страны (например, Японии 81...)
            
            log.info("Telegram notification phone: {}", cleanPhone);
            
            try {
                notificationClient.sendTelegramMessage(internalSecret, Map.of(
                    "tenantId", tenantId,
                    "phone", cleanPhone,
                    "text", message
                ));
            } catch (Exception e) {
                log.error("Failed to send to microservice: {}", e.getMessage());
                throw e;
            }
        } else {
            log.warn("Cannot send Telegram notification: No phone found for contact");
        }
    }
}
