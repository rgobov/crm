package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.repository.WappiSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final ContactRepository contactRepository;
    private final WappiSettingsRepository settingsRepository;
    private final WappiService wappiService;

    @Scheduled(fixedRateString = "${reminder.check.interval:60000}")
    public void checkAndSendReminders() {
        List<WappiSettings> allSettings = settingsRepository.findAll();
        
        for (WappiSettings settings : allSettings) {
            if (!settings.isEnabled() || settings.getApiKey() == null) continue;

            List<Appointment> appointments = appointmentRepository.findPendingReminders(
                settings.getTenantId(), 
                LocalDate.now()
            );
            
            LocalDateTime now = LocalDateTime.now();

            for (Appointment app : appointments) {
                LocalDateTime appointmentTime = app.getDate().atTime(app.getTime());
                
                // --- ИЗМЕНЕНИЕ: Не шлем напоминания, если до записи осталось меньше 10 минут ---
                // Это защищает от спама по старым записям при перезагрузке сервера.
                if (appointmentTime.isAfter(now.plusMinutes(10)) && 
                    appointmentTime.isBefore(now.plusMinutes(settings.getLeadTimeMinutes()))) {
                    
                    contactRepository.findById(app.getContactId()).ifPresent(contact -> {
                        try {
                            wappiService.sendReminder(app, contact);
                            app.setReminderSent(true);
                            appointmentRepository.save(app);
                            System.out.println("SUCCESS: TAPI reminder queued for: " + contact.getName());
                        } catch (Exception e) {
                            System.err.println("ERROR: Failed to send TAPI reminder: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
