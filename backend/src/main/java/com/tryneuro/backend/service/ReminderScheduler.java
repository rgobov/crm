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
        // Берем будущие записи, где напоминание не отправлено и разрешено
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
            
            // ЧИТАЕМ ИНДИВИДУАЛЬНУЮ НАСТРОЙКУ ЧАСОВ ИЗ ЗАПИСИ
            int leadTimeHours = app.getReminderLeadTimeHours() != null ? app.getReminderLeadTimeHours() : 24;

            // Если до записи осталось меньше или равно leadTimeHours, но больше 30 минут
            if (startTime.isBefore(now.plusHours(leadTimeHours)) && startTime.isAfter(now.plusMinutes(30))) {
                try {
                    log.info("⏰ Time to send reminder for appointment {}: за {}ч. до визита", app.getId(), leadTimeHours);
                    notificationManager.sendNotification(app, "REMINDER");
                    
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                } catch (Exception e) {
                    log.error("❌ Error sending reminder: {}", e.getMessage());
                }
            } else if (startTime.isBefore(now.plusMinutes(30))) {
                // Очистка просроченных
                app.setReminderSent(true);
                appointmentRepository.save(app);
            }
        }
    }
}
