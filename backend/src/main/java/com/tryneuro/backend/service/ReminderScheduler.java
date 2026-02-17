package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationManager notificationManager;

    @Scheduled(fixedRateString = "${reminder.check.interval:60000}")
    @Transactional
    public void checkAndSendReminders() {
        List<Appointment> pendingAppointments = appointmentRepository.findAllByReminderSentFalseAndAllowReminderTrueAndStartTimeAfter(
            OffsetDateTime.now()
        );

        if (pendingAppointments.isEmpty()) return;

        OffsetDateTime now = OffsetDateTime.now();

        for (Appointment app : pendingAppointments) {
            // Только для Ожидается или Подтвержден
            if (app.getStatus() != AppointmentStatus.SCHEDULED && app.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }

            OffsetDateTime startTime = app.getStartTime();
            int leadTimeHours = app.getReminderLeadTimeHours() != null ? app.getReminderLeadTimeHours() : 24;

            if (startTime.isBefore(now.plusHours(leadTimeHours)) && startTime.isAfter(now.plusMinutes(30))) {
                try {
                    log.info("⏰ Triggering scheduled reminder for appointment: {}", app.getId());
                    notificationManager.sendNotification(app, "REMINDER");
                    
                    // Успешная отправка
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                } catch (Exception e) {
                    // ЕСЛИ ОШИБКА: Всё равно помечаем как отправленное, чтобы не зацикливать бан.
                    // В логах увидим причину.
                    log.error("❌ Failed to process reminder for {}: {}. Mark as skipped to avoid spam.", app.getId(), e.getMessage());
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                }
            } else if (startTime.isBefore(now.plusMinutes(30))) {
                // Если клиент уже почти пришел - просто гасим напоминание
                app.setReminderSent(true);
                appointmentRepository.save(app);
            }
        }
    }
}
