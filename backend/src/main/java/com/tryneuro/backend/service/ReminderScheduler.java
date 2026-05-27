package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
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

    @Scheduled(fixedRateString = "${reminder.check.interval:60000}")
    public void checkAndSendReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Получаем максимальный интервал напоминаний среди ожидающих записей
        Integer maxHours = appointmentRepository.findMaxReminderLeadTimeHours();
        int hoursToFetch = (maxHours != null) ? maxHours : 24;

        // Ограничение безопасности: максимум 30 дней (720 часов), чтобы избежать ошибок ввода и переполнения
        if (hoursToFetch > 720) {
            log.warn("⚠️ Found reminder lead time of {} hours in DB, capping query window at 720 hours (30 days) for safety.", hoursToFetch);
            hoursToFetch = 720;
        }

        OffsetDateTime maxStartTime = now.plusHours(hoursToFetch + 1);

        // Получаем будущие записи в пределах окна упреждения, где напоминание еще не отправлено
        List<Appointment> pendingAppointments = appointmentRepository.findAppointmentsForReminders(
            now,
            maxStartTime
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
                    
                    appointmentRepository.updateReminderSentStatus(app.getId(), true);
                } catch (Exception e) {
                    log.error("❌ Failed to send reminder for {}: {}. Will retry later.", app.getId(), e.getMessage());
                    // Оставляем reminderSent = false, чтобы система попробовала снова в следующем цикле
                }
            } else if (startTime.isBefore(now.plusMinutes(5))) {
                // Если до визита осталось меньше 5 минут или он уже начался - 
                // помечаем как "пропущенное", чтобы не пугать клиента за минуту до встречи.
                log.warn("⏳ Appointment {} is too close or already started. Marking reminder as skipped.", app.getId());
                appointmentRepository.updateReminderSentStatus(app.getId(), true);
            }
        }
    }
}
