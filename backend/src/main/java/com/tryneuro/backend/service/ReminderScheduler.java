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
        OffsetDateTime now = OffsetDateTime.now();
        
        // Получаем все будущие записи, где напоминание еще не отправлено
        List<Appointment> pendingAppointments = appointmentRepository.findAllByReminderSentFalseAndAllowReminderTrueAndStartTimeAfter(
            now
        );

        if (pendingAppointments.isEmpty()) return;

        for (Appointment app : pendingAppointments) {
            // Только для активных статусов
            if (app.getStatus() != AppointmentStatus.SCHEDULED && app.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }

            OffsetDateTime startTime = app.getStartTime();
            int leadTimeHours = app.getReminderLeadTimeHours() != null ? app.getReminderLeadTimeHours() : 24;
            
            // Время, когда нужно отправить напоминание (время визита МИНУС часы упреждения)
            OffsetDateTime triggerTime = startTime.minusHours(leadTimeHours);

            // Если время отправки уже наступило (или прошло), но до визита еще есть хотя бы 5 минут
            if (now.isAfter(triggerTime) && startTime.isAfter(now.plusMinutes(5))) {
                try {
                    log.info("🚀 Triggering reminder for appointment {}. Client: {}", app.getId(), app.getClientName());
                    notificationManager.sendNotification(app, "REMINDER");
                    
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                } catch (Exception e) {
                    log.error("❌ Failed to send reminder for {}: {}. Will retry later.", app.getId(), e.getMessage());
                    // Оставляем reminderSent = false, чтобы система попробовала снова в следующем цикле
                }
            } else if (startTime.isBefore(now.plusMinutes(5))) {
                // Если до визита осталось меньше 5 минут или он уже начался - 
                // помечаем как "пропущенное", чтобы не пугать клиента за минуту до встречи.
                log.warn("⏳ Appointment {} is too close or already started. Marking reminder as skipped.", app.getId());
                app.setReminderSent(true);
                appointmentRepository.save(app);
            }
        }
    }
}
