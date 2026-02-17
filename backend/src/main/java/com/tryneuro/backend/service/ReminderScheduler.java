package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.NotificationTemplate;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationManager notificationManager;

    @Scheduled(fixedRateString = "${reminder.check.interval:60000}")
    @Transactional
    public void checkAndSendReminders() {
        // Берем все записи, где напоминание не отправлено и разрешено
        List<Appointment> pendingAppointments = appointmentRepository.findAllByReminderSentFalseAndAllowReminderTrueAndStartTimeAfter(
            OffsetDateTime.now()
        );

        if (pendingAppointments.isEmpty()) return;

        Map<String, List<Appointment>> byTenant = pendingAppointments.stream()
                .collect(Collectors.groupingBy(Appointment::getTenantId));

        OffsetDateTime now = OffsetDateTime.now();

        for (Map.Entry<String, List<Appointment>> entry : byTenant.entrySet()) {
            String tenantId = entry.getKey();
            
            int leadTimeHours = templateRepository.findByTenantIdAndType(tenantId, "REMINDER")
                    .map(NotificationTemplate::getLeadTimeHours)
                    .orElse(24);

            for (Appointment app : entry.getValue()) {
                // ПРОВЕРКА СТАТУСА: Только Ожидается или Подтвержден
                if (app.getStatus() != AppointmentStatus.SCHEDULED && app.getStatus() != AppointmentStatus.CONFIRMED) {
                    continue; 
                }

                OffsetDateTime startTime = app.getStartTime();
                
                // Проверка окна времени (от 30 мин до leadTimeHours)
                if (startTime.isBefore(now.plusHours(leadTimeHours)) && startTime.isAfter(now.plusMinutes(30))) {
                    try {
                        log.info("⏰ Sending reminder for upcoming appointment: {} (Status: {})", app.getId(), app.getStatus());
                        notificationManager.sendNotification(app, "REMINDER");
                        
                        app.setReminderSent(true);
                        appointmentRepository.save(app);
                    } catch (Exception e) {
                        log.error("❌ Failed to process reminder: {}", e.getMessage());
                    }
                } else if (startTime.isBefore(now.plusMinutes(30))) {
                    // Очистка просроченных
                    app.setReminderSent(true);
                    appointmentRepository.save(app);
                }
            }
        }
    }
}
