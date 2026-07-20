package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.dto.ReturnReminderCandidate;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnReminderService {

    private final AppointmentRepository appointmentRepository;
    private final ContactRepository contactRepository;
    private final NotificationClient notificationClient;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    public List<ReturnReminderCandidate> getCandidates(String tenantId, int daysThreshold) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(daysThreshold);
        List<Object[]> rows = appointmentRepository.findReturnReminderCandidates(tenantId, cutoff);
        List<ReturnReminderCandidate> result = new ArrayList<>();

        for (Object[] row : rows) {
            String contactId = (String) row[0];
            String name = (String) row[1];
            String phone = (String) row[2];
            String lastService = (String) row[3];
            Timestamp ts = (Timestamp) row[4];
            LocalDateTime lastVisit = ts != null ? ts.toLocalDateTime() : null;

            long daysSince = lastVisit != null
                ? ChronoUnit.DAYS.between(lastVisit, LocalDateTime.now())
                : 0;

            result.add(ReturnReminderCandidate.builder()
                    .contactId(contactId)
                    .name(name)
                    .phone(phone)
                    .lastService(lastService)
                    .lastVisit(lastVisit)
                    .daysSinceLastVisit(daysSince)
                    .build());
        }

        return result;
    }

    public long getCount(String tenantId, int daysThreshold) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(daysThreshold);
        return appointmentRepository.countReturnReminderCandidates(tenantId, cutoff);
    }

    public Map<String, Object> sendReminder(String contactId, String message, String tenantId) {
        Contact contact = contactRepository.findById(contactId).orElse(null);
        if (contact == null) {
            return Map.of("success", false, "error", "Клиент не найден");
        }

        String phone = contact.getPhones() != null && !contact.getPhones().isEmpty()
                ? contact.getPhones().get(0) : null;

        if (phone == null || phone.isEmpty()) {
            return Map.of("success", false, "error", "У клиента нет номера телефона");
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 11 && cleanPhone.startsWith("8")) {
            cleanPhone = "7" + cleanPhone.substring(1);
        }

        try {
            Map<String, String> requestData = new HashMap<>();
            requestData.put("tenantId", tenantId);
            requestData.put("phone", cleanPhone);
            requestData.put("name", contact.getName());
            requestData.put("text", message);

            notificationClient.sendTelegramMessage(internalSecret, requestData);
            log.info("✅ Return reminder sent to {} ({})", contact.getName(), cleanPhone);
            return Map.of("success", true);
        } catch (Exception e) {
            log.error("❌ Return reminder failed for {}: {}", cleanPhone, e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
