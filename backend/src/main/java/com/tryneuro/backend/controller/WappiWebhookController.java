package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.repository.WappiSettingsRepository;
import com.tryneuro.backend.service.WappiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/wappi")
@RequiredArgsConstructor
public class WappiWebhookController {

    private final AppointmentRepository appointmentRepository;
    private final ContactRepository contactRepository;
    private final WappiSettingsRepository settingsRepository;
    private final WappiService wappiService;

    @PostMapping
    public void handleWappiEvent(@RequestBody Map<String, Object> payload) {
        // ЛОГ №1: Видим ВЕСЬ входящий запрос (очень важно для отладки)
        System.out.println("WEBHOOK DEBUG: Received payload: " + payload);

        // 1. Извлекаем данные сообщения
        Map<String, Object> data = extractMessageData(payload);
        if (data == null) {
            System.out.println("WEBHOOK DEBUG: No message data found.");
            return;
        }

        // 2. Ищем profile_id (критично для Multi-tenancy)
        String profileId = extractProfileId(payload, data);
        if (profileId == null) {
            System.out.println("WEBHOOK DEBUG: profile_id not found in JSON.");
            return;
        }

        // 3. Находим компанию по профилю
        Optional<WappiSettings> settingsOpt = settingsRepository.findByProfileId(profileId);
        if (settingsOpt.isEmpty()) {
            System.out.println("WEBHOOK DEBUG: Profile " + profileId + " not registered in our CRM.");
            return;
        }
        WappiSettings settings = settingsOpt.get();
        String tenantId = settings.getTenantId();

        // 4. Извлекаем номер телефона и текст (приоритет полю phone)
        String rawPhone = (String) data.getOrDefault("phone", data.get("sender"));
        String messageBody = (String) data.get("body");

        if (rawPhone == null || messageBody == null || messageBody.trim().isEmpty()) {
            System.out.println("WEBHOOK DEBUG: Phone or body is null/empty.");
            return;
        }

        String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
        String text = messageBody.toLowerCase().trim();

        // 5. Парсинг команд
        boolean isConfirm = text.contains("да") || text.contains("буду") || text.contains("confirm") || text.contains("✅");
        boolean isCancel = text.contains("нет") || text.contains("отмен") || text.contains("cancel") || text.contains("❌");

        if (!isConfirm && !isCancel) {
            System.out.println("WEBHOOK DEBUG: No keywords in text: " + text);
            return;
        }

        // 6. Ищем контакт внутри этой компании
        List<Contact> tenantContacts = contactRepository.findByTenantId(tenantId);
        Optional<Contact> contactOpt = tenantContacts.stream()
                .filter(c -> c.getPhones().stream().anyMatch(p -> p.replaceAll("[^0-9]", "").equals(cleanPhone)))
                .findFirst();

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            System.out.println("WEBHOOK DEBUG: Found contact " + contact.getName() + " for phone " + cleanPhone);

            // 7. Находим ПОСЛЕДНЮЮ запись клиента
            List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contact.getId(), tenantId);
            
            if (!apps.isEmpty()) {
                Appointment latestApp = apps.get(0);
                System.out.println("WEBHOOK DEBUG: Found latest app " + latestApp.getId() + " with status " + latestApp.getStatus());

                // ОБНОВЛЯЕМ (Убрали проверку на SCHEDULED для гибкости)
                if (isConfirm) {
                    latestApp.setStatus(AppointmentStatus.CONFIRMED);
                    wappiService.sendMessage(settings, cleanPhone, "Спасибо! Запись подтверждена. 👍");
                } else {
                    latestApp.setStatus(AppointmentStatus.NEEDS_CALL);
                    wappiService.sendMessage(settings, cleanPhone, "Запись отменена. Мы свяжемся с вами. 📞");
                }
                
                appointmentRepository.save(latestApp);
                System.out.println("WEBHOOK SUCCESS: Updated status for " + contact.getName() + " to " + latestApp.getStatus());
            } else {
                System.out.println("WEBHOOK DEBUG: No appointments found for contact " + contact.getName());
            }
        } else {
            System.out.println("WEBHOOK DEBUG: Phone " + cleanPhone + " not found in contacts for Tenant " + tenantId);
        }
    }

    private String extractProfileId(Map<String, Object> payload, Map<String, Object> data) {
        if (payload.containsKey("profile_id")) return String.valueOf(payload.get("profile_id"));
        if (data != null && data.containsKey("profile_id")) return String.valueOf(data.get("profile_id"));
        return null;
    }

    private Map<String, Object> extractMessageData(Map<String, Object> payload) {
        Object messagesObj = payload.get("messages");
        if (messagesObj instanceof List && !((List<?>) messagesObj).isEmpty()) {
            return (Map<String, Object>) ((List<?>) messagesObj).get(0);
        } else if (messagesObj instanceof Map) {
            return (Map<String, Object>) messagesObj;
        }
        return (Map<String, Object>) payload.get("data");
    }
}
