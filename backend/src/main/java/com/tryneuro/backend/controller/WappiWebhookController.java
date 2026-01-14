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
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        // Игнорируем файлы и медиа
        if (data.containsKey("file") || data.containsKey("media")) return;

        String phone = (String) data.get("sender");
        String messageBody = (String) data.get("body");

        if (phone == null || messageBody == null || messageBody.trim().isEmpty()) return;

        // ПАРСИНГ ОТВЕТА (Регистронезависимый)
        String text = messageBody.toLowerCase().trim();
        
        boolean isConfirm = text.contains("да") || text.contains("буду") || 
                           text.contains("подтвержд") || text.contains("confirm") || 
                           text.contains("✅");
        
        boolean isCancel = text.contains("нет") || text.contains("не смогу") || 
                          text.contains("отмен") || text.contains("cancel") || 
                          text.contains("❌");

        if (!isConfirm && !isCancel) return;

        // Поиск клиента
        List<Contact> allContacts = contactRepository.findAll();
        Optional<Contact> contactOpt = allContacts.stream()
                .filter(c -> c.getPhones().stream().anyMatch(p -> p.replaceAll("[^0-9]", "").equals(phone)))
                .findFirst();

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contact.getId(), contact.getTenantId());
            
            if (!apps.isEmpty()) {
                Appointment latestApp = apps.get(0);
                
                // Настройки для авто-ответа
                WappiSettings settings = settingsRepository.findByTenantId(contact.getTenantId()).orElse(null);
                
                if (isConfirm) {
                    latestApp.setStatus(AppointmentStatus.CONFIRMED);
                    if (settings != null) {
                        wappiService.sendMessage(settings, phone, "Спасибо! Ваша запись подтверждена. До встречи! 💅✨");
                    }
                } else {
                    latestApp.setStatus(AppointmentStatus.NEEDS_CALL);
                    if (settings != null) {
                        wappiService.sendMessage(settings, phone, "Запись отменена. Мы скоро свяжемся с вами, чтобы подобрать другое время. 📞");
                    }
                }
                
                appointmentRepository.save(latestApp);
                System.out.println("SUCCESS: Processed keyword response from " + contact.getName() + ": " + text);
            }
        }
    }
}
