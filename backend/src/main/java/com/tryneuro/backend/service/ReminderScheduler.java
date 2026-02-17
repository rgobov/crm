package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationManager notificationManager;

    /**
     * Проверка и отправка напоминаний каждые 60 секунд.
     * Ищет записи, которые начнутся скоро (например, через 2 часа).
     */
    @Scheduled(fixedRateString = "${reminder.check.interval:60000}")
    public void checkAndSendReminders() {
        // Используем правильный метод репозитория с учетом OffsetDateTime
        List<Appointment> pendingAppointments = appointmentRepository.findAllByReminderSentFalseAndAllowReminderTrueAndStartTimeAfter(
            OffsetDateTime.now()
        );

        OffsetDateTime now = OffsetDateTime.now();
        int leadTimeMinutes = 120; // Время упреждения (2 часа)

        for (Appointment app : pendingAppointments) {
            OffsetDateTime startTime = app.getStartTime();
            
            // Если до записи осталось меньше 120 минут, но больше 10 минут
            if (startTime.isBefore(now.plusMinutes(leadTimeMinutes)) && startTime.isAfter(now.plusMinutes(10))) {
                try {
                    log.info("⏰ Time to send reminder for appointment: {}", app.getId());
                    notificationManager.sendNotification(app, "REMINDER_2_HOURS");
                    
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                } catch (Exception e) {
                    log.error("❌ Failed to send scheduled reminder: {}", e.getMessage());
                }
            }
        }
    }
}
