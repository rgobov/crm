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
        // Логируем только важные события, чтобы не захламлять консоль
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        String buttonId = (String) data.get("button_id");
        String phone = (String) data.get("sender");

        if (buttonId == null || phone == null) return;

        System.out.println("DEBUG: Wappi Webhook - Button '" + buttonId + "' clicked by " + phone);

        // Поиск контакта по любому из номеров в массиве
        List<Contact> allContacts = contactRepository.findAll();
        Optional<Contact> contactOpt = allContacts.stream()
                .filter(c -> c.getPhones().stream().anyMatch(p -> p.replaceAll("[^0-9]", "").equals(phone)))
                .findFirst();

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            // Находим последнюю запись клиента (по которой пришло напоминание)
            List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contact.getId(), contact.getTenantId());
            
            if (!apps.isEmpty()) {
                Appointment latestApp = apps.get(0);
                if ("confirm".equals(buttonId)) {
                    latestApp.setStatus(AppointmentStatus.CONFIRMED);
                } else if ("cancel".equals(buttonId)) {
                    latestApp.setStatus(AppointmentStatus.NEEDS_CALL);
                }
                appointmentRepository.save(latestApp);
                System.out.println("SUCCESS: Appointment status updated to " + latestApp.getStatus() + " for " + contact.getName());
            }
        }
    }
}
