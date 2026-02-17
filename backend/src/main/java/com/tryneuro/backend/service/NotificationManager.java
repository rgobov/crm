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

    /**
     * Отправка уведомления по типу события
     */
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

        log.info("Preparing to send Telegram notification for tenant {}: {}", tenantId, message);
        
        // ПРОВЕРКА КОНТАКТА И ТЕЛЕФОНОВ
        if (appointment.getContact() != null && 
            appointment.getContact().getPhones() != null && 
            !appointment.getContact().getPhones().isEmpty()) {
            
            // Берем первый номер из списка text[]
            String rawPhone = appointment.getContact().getPhones().get(0);
            String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
            
            log.info("Sending message to phone: {}", cleanPhone);
            
            notificationClient.sendTelegramMessage(internalSecret, Map.of(
                "tenantId", tenantId,
                "phone", cleanPhone,
                "text", message
            ));
        } else {
            log.warn("Cannot send Telegram notification: No phone found for contact in appointment {}", appointment.getId());
        }
    }
}
