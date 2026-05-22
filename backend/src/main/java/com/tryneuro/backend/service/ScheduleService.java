package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.BranchRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final AppointmentRepository appointmentRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final BranchRepository branchRepository;
    private final ContactService contactService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ScheduleService(AppointmentRepository appointmentRepository, 
                           StaffShiftRepository staffShiftRepository,
                           BranchRepository branchRepository,
                           ContactService contactService,
                           SimpMessagingTemplate messagingTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.branchRepository = branchRepository;
        this.contactService = contactService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Обогащает запись визита телефоном из контакта, если он отсутствует.
     * Это решает проблему отображения для старых записей.
     */
    private void enrichAppointmentPhone(Appointment app) {
        if ((app.getClientPhone() == null || app.getClientPhone().isEmpty()) && app.getContactId() != null) {
            contactService.getContactById(app.getContactId()).ifPresent(contact -> {
                if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                    app.setClientPhone(contact.getPhones().get(0));
                }
            });
        }
    }

    @Transactional
    public Appointment addAppointment(Appointment appointment) {
        log.info("🚀 [ADD] Creating appointment for client '{}' in branch '{}'", appointment.getClientName(), appointment.getBranchId());
        
        // Гарантируем наличие телефона перед сохранением
        enrichAppointmentPhone(appointment);
        
        validateAvailability(appointment);
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getContactId() != null && saved.getReferenceTag() != null && !saved.getReferenceTag().isEmpty()) {
            contactService.addTagIfMissing(saved.getContactId(), saved.getReferenceTag());
        }
        notifyAppointmentChange(saved, "CREATED");
        return saved;
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details) {
        log.info("🔄 [UPDATE] Saving changes for appointment ID: {}. Client: {}", id, details.getClientName());
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        // Проверяем изменились ли критические поля для валидации доступности
        boolean timeChanged = !appointment.getStartTime().equals(details.getStartTime());
        boolean durationChanged = !appointment.getDurationInMinutes().equals(details.getDurationInMinutes());
        boolean staffChanged = !
                Objects.equals(appointment.getStaffMemberId(), details.getStaffMemberId());
        boolean branchChanged = !Objects.equals(appointment.getBranchId(), details.getBranchId());
        boolean resourceChanged = !Objects.equals(appointment.getResourceId(), details.getResourceId());
        
        // Обновляем все поля
        appointment.setStartTime(details.getStartTime());
        appointment.setDurationInMinutes(details.getDurationInMinutes());
        appointment.setClientName(details.getClientName());
        appointment.setClientPhone(details.getClientPhone()); // Обновляем телефон
        appointment.setContactId(details.getContactId());
        appointment.setService(details.getService());
        appointment.setStaffMemberId(details.getStaffMemberId());
        appointment.setResourceId(details.getResourceId());
        appointment.setStatus(details.getStatus());
        appointment.setComment(details.getComment());
        appointment.setAllowReminder(details.isAllowReminder());
        appointment.setReminderLeadTimeHours(details.getReminderLeadTimeHours());
        appointment.setReferenceTag(details.getReferenceTag());
        
        if (details.getBranchId() != null) appointment.setBranchId(details.getBranchId());

        // Если телефон всё ещё пуст после апдейта фронтендом - подтягиваем из контакта
        enrichAppointmentPhone(appointment);

        // Валидируем доступность только если изменились время, длительность, мастер, филиал или ресурс
        if (timeChanged || durationChanged || staffChanged || branchChanged || resourceChanged) {
            validateAvailability(appointment);
        }
        
        Appointment updated = appointmentRepository.save(appointment);
        if (updated.getContactId() != null && updated.getReferenceTag() != null && !updated.getReferenceTag().isEmpty()) {
            contactService.addTagIfMissing(updated.getContactId(), updated.getReferenceTag());
        }
        notifyAppointmentChange(updated, "UPDATED");
        return updated;
    }

    public List<Appointment> getAppointmentsForDay(LocalDate date, String tenantId, String branchId) {
        List<Appointment> apps = appointmentRepository.findByDateAndTenantIdAndBranchId(date, tenantId, branchId);
        // Обогащаем КАЖДУЮ запись телефоном перед отправкой на фронтенд
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    private void validateAvailability(Appointment app) {
        String timezone = branchRepository.findById(app.getBranchId())
                .map(b -> b.getTimezone())
                .orElse("Europe/Moscow");

        ZonedDateTime branchDateTime = app.getStartTime().atZoneSameInstant(ZoneId.of(timezone));
        LocalDate localDate = branchDateTime.toLocalDate();
        LocalTime localTime = branchDateTime.toLocalTime();

        String appId = (app.getId() == null || app.getId().equals("new")) ? null : app.getId();

        // Проверяем доступность мастера
        if (app.getStaffMemberId() != null && !isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), localDate, localTime, app.getDurationInMinutes(), appId, app.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Мастер занят или не работает в этом филиале в это время");
        }

        // Проверяем доступность ресурса
        if (app.getResourceId() != null && !isResourceAvailable(app.getTenantId(), app.getResourceId(), localDate, localTime, app.getDurationInMinutes(), appId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс занят в это время");
        }
    }

    public boolean isStaffMemberAvailable(String tenantId, String staffId, LocalDate date, LocalTime time, int duration, String currentAppId, String branchId) {
        Optional<StaffShift> shiftInBranch = staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId);
        
        if (shiftInBranch.isEmpty() || shiftInBranch.get().isDayOff()) return false;

        StaffShift shift = shiftInBranch.get();
        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        
        LocalTime sStart = shift.getWorkStartTime();
        LocalTime sEnd = shift.getWorkEndTime();

        boolean withinHours;
        if (sEnd.isAfter(sStart)) {
            withinHours = !start.isBefore(sStart) && !end.isAfter(sEnd);
        } else {
            withinHours = !start.isBefore(sStart) || !end.isAfter(sEnd);
        }

        if (!withinHours) return false;

        if (shift.getBreakStartTime() != null && shift.getBreakEndTime() != null) {
            if (start.isBefore(shift.getBreakEndTime()) && end.isAfter(shift.getBreakStartTime())) return false;
        }

        List<Appointment> allStaffApps = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
        for (Appointment existing : allStaffApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }

    public boolean isResourceAvailable(String tenantId, String resourceId, LocalDate date, LocalTime time, int duration, String currentAppId) {
        List<Appointment> allResourceApps = appointmentRepository.findByResourceIdAndDate(resourceId, date);
        
        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        
        for (Appointment existing : allResourceApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        List<Appointment> apps = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public void deleteAppointment(String id) {
        appointmentRepository.findById(id).ifPresent(app -> {
            // Сначала уведомляем клиентов об удалении (пока данные есть в объекте)
            notifyAppointmentChange(app, "DELETED");
            
            // Затем удаляем из БД
            appointmentRepository.deleteById(id);
        });
    }

    private void notifyAppointmentChange(Appointment app, String changeType) {
        if (app != null && app.getTenantId() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "APPOINTMENT_" + changeType);
            payload.put("appointmentId", app.getId());
            payload.put("branchId", app.getBranchId());
            payload.put("date", app.getDate().toString());
            payload.put("timestamp", System.currentTimeMillis());
            
            if (!"DELETED".equals(changeType)) {
                payload.put("staffId", app.getStaffMemberId());
                payload.put("status", app.getStatus());
            }

            // ✅ Отправляем ТОЛЬКО после коммита транзакции
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/schedule/" + app.getTenantId(), payload);
                    }
                });
            } else {
                messagingTemplate.convertAndSend("/topic/schedule/" + app.getTenantId(), payload);
            }
        }
    }

    private void notifyChange(String tenantId, String type, String branchId, LocalDate date) {
        if (tenantId == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type != null ? type : "SCHEDULE_UPDATED");
        payload.put("timestamp", System.currentTimeMillis());
        if (branchId != null) payload.put("branchId", branchId);
        if (date != null) payload.put("date", date.toString());

        // ✅ Отправляем ТОЛЬКО после коммита транзакции
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
        }
    }

    private void notifyChange(String tenantId) {
        notifyChange(tenantId, "SCHEDULE_UPDATED", null, null);
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month, String branchId) {
        if (branchId == null || branchId.isEmpty() || "null".equals(branchId)) {
            return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
        } else {
            return appointmentRepository.getWorkloadForMonthAndBranch(tenantId, year, month, branchId);
        }
    }
}
