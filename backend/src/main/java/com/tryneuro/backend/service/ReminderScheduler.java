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
        System.out.println("Running reminder check...");
        
        List<WappiSettings> allSettings = settingsRepository.findAll();
        
        for (WappiSettings settings : allSettings) {
            if (!settings.isEnabled() || settings.getApiKey() == null) continue;

            List<Appointment> appointments = appointmentRepository.findByTenantId(settings.getTenantId());
            LocalDateTime now = LocalDateTime.now();

            for (Appointment app : appointments) {
                // --- ИСПРАВЛЕНИЕ: Используем правильный геттер getReminderSent() и проверяем на null ---
                boolean alreadySent = app.getReminderSent() != null && app.getReminderSent();
                
                if (alreadySent || app.getContactId() == null) continue;

                LocalDateTime appointmentTime = app.getDate().atTime(app.getTime());
                
                if (appointmentTime.isBefore(now.plusMinutes(settings.getLeadTimeMinutes())) 
                    && appointmentTime.isAfter(now)) {
                    
                    contactRepository.findById(app.getContactId()).ifPresent(contact -> {
                        try {
                            wappiService.sendReminder(app, contact);
                            app.setReminderSent(true);
                            appointmentRepository.save(app);
                            System.out.println("SUCCESS: Reminder sent to " + contact.getName());
                        } catch (Exception e) {
                            System.err.println("ERROR: Failed to send reminder: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
