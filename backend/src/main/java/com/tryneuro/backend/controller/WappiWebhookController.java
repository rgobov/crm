package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
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

    @PostMapping
    public void handleWappiEvent(@RequestBody Map<String, Object> payload) {
        // Извлекаем данные сообщения
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        String phone = (String) data.get("sender");
        String messageBody = (String) data.get("body");

        if (phone == null || messageBody == null) return;

        // --- ЛОГИКА РАСПОЗНАВАНИЯ КОМАНД ---
        // Ищем в тексте сообщения наши команды или простые ответы
        boolean isConfirm = messageBody.contains("/confirm") || 
                           messageBody.equalsIgnoreCase("Да") || 
                           messageBody.equalsIgnoreCase("Подтверждаю");
        
        boolean isCancel = messageBody.contains("/cancel") || 
                          messageBody.equalsIgnoreCase("Нет") || 
                          messageBody.equalsIgnoreCase("Отмена");

        if (!isConfirm && !isCancel) return;

        System.out.println("DEBUG: Webhook - Processed signal from " + phone + ". Action: " + (isConfirm ? "CONFIRM" : "CANCEL"));

        // Ищем клиента по номеру телефона
        List<Contact> allContacts = contactRepository.findAll();
        Optional<Contact> contactOpt = allContacts.stream()
                .filter(c -> c.getPhones().stream().anyMatch(p -> p.replaceAll("[^0-9]", "").equals(phone)))
                .findFirst();

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            // Находим последнюю запись этого клиента
            List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contact.getId(), contact.getTenantId());
            
            if (!apps.isEmpty()) {
                Appointment latestApp = apps.get(0);
                
                if (isConfirm) {
                    latestApp.setStatus(AppointmentStatus.CONFIRMED);
                } else {
                    latestApp.setStatus(AppointmentStatus.NEEDS_CALL);
                }
                
                appointmentRepository.save(latestApp);
                System.out.println("SUCCESS: Status updated for " + contact.getName());
            }
        }
    }
}
