package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public List<Appointment> getAppointmentsForDay(LocalDate date, String tenantId, String branchId) {
        return appointmentRepository.findByDateAndTenantIdAndBranchId(date, tenantId, branchId);
    }

    @Transactional
    public Appointment addAppointment(Appointment appointment) {
        log.info("🚀 [ADD] Creating appointment for client '{}' in branch '{}'", appointment.getClientName(), appointment.getBranchId());
        validateAvailability(appointment);
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getContactId() != null && saved.getReferenceTag() != null && !saved.getReferenceTag().isEmpty()) {
            contactService.addTagIfMissing(saved.getContactId(), saved.getReferenceTag());
        }
        notifyChange(saved.getTenantId());
        return saved;
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details) {
        log.info("🔄 [UPDATE] Saving changes for appointment ID: {}. Client: {}", id, details.getClientName());
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        appointment.setStartTime(details.getStartTime());
        appointment.setDurationInMinutes(details.getDurationInMinutes());
        appointment.setClientName(details.getClientName());
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

        validateAvailability(appointment);
        Appointment updated = appointmentRepository.save(appointment);
        if (updated.getContactId() != null && updated.getReferenceTag() != null && !updated.getReferenceTag().isEmpty()) {
            contactService.addTagIfMissing(updated.getContactId(), updated.getReferenceTag());
        }
        notifyChange(updated.getTenantId());
        return updated;
    }

    private void validateAvailability(Appointment app) {
        if (app.getStaffMemberId() == null) {
            log.info("⚠️ [AVAIL] No staff member assigned, skipping availability check.");
            return;
        }

        // 1. Получаем таймзону филиала надежно
        String timezone = branchRepository.findById(app.getBranchId())
                .map(b -> b.getTimezone())
                .orElse("Europe/Moscow");

        // 2. Вычисляем ЛОКАЛЬНУЮ дату и время записи
        ZonedDateTime branchDateTime = app.getStartTime().atZoneSameInstant(ZoneId.of(timezone));
        LocalDate localDate = branchDateTime.toLocalDate();
        LocalTime localTime = branchDateTime.toLocalTime();

        log.info("🔍 [AVAIL] Checking for staff: {}, Branch: {} ({})", app.getStaffMemberId(), app.getBranchId(), timezone);
        log.info("🔍 [AVAIL] Appointment Start (UTC): {}, Localized: {} {}", app.getStartTime(), localDate, localTime);

        String appId = (app.getId() == null || app.getId().equals("new")) ? null : app.getId();

        if (!isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), localDate, localTime, app.getDurationInMinutes(), appId, app.getBranchId())) {
            log.error("❌ [AVAIL] CONFLICT DETECTED for staff {}. Time: {} {}", app.getStaffMemberId(), localDate, localTime);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Мастер занят или не работает в этом филиале в это время");
        }
        log.info("✅ [AVAIL] No conflicts found.");
    }

    public boolean isStaffMemberAvailable(String tenantId, String staffId, LocalDate date, LocalTime time, int duration, String currentAppId, String branchId) {
        Optional<StaffShift> shiftInBranch = staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId);
        
        if (shiftInBranch.isEmpty()) {
            log.warn("❌ [CHECK] Master {} HAS NO SHIFT on {} in branch {}", staffId, date, branchId);
            return false;
        }
        
        StaffShift shift = shiftInBranch.get();
        if (shift.isDayOff()) {
            log.warn("❌ [CHECK] Master {} has a DAY OFF on {}", staffId, date);
            return false;
        }

        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        
        log.info("📊 [CHECK] App Range: {} - {}. Master Shift: {} - {}", start, end, shift.getWorkStartTime(), shift.getWorkEndTime());

        // Проверка вхождения в рабочий график
        if (start.isBefore(shift.getWorkStartTime()) || end.isAfter(shift.getWorkEndTime())) {
            log.warn("❌ [CHECK] OUTSIDE WORKING HOURS. App: {}-{}, Shift: {}-{}", start, end, shift.getWorkStartTime(), shift.getWorkEndTime());
            return false;
        }

        // Проверка на перерыв
        if (shift.getBreakStartTime() != null && shift.getBreakEndTime() != null) {
            if (start.isBefore(shift.getBreakEndTime()) && end.isAfter(shift.getBreakStartTime())) {
                log.warn("❌ [CHECK] CONFLICT WITH BREAK: {} - {}", shift.getBreakStartTime(), shift.getBreakEndTime());
                return false;
            }
        }

        // Проверка на пересечение с другими записями
        List<Appointment> allStaffApps = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
        for (Appointment existing : allStaffApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            
            // ВАЖНО: Существующие записи тоже пересчитываем в лок. время для сравнения
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            
            if (start.isBefore(eEnd) && end.isAfter(eStart)) {
                log.warn("❌ [CHECK] CONFLICT WITH APPOINTMENT ID: {}. Range: {} - {}", existing.getId(), eStart, eEnd);
                return false;
            }
        }
        return true;
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        return appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        return appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
    }

    @Transactional
    public void deleteAppointment(String id) {
        appointmentRepository.findById(id).ifPresent(app -> {
            String tenantId = app.getTenantId();
            appointmentRepository.deleteById(id);
            notifyChange(tenantId);
        });
    }

    private void notifyChange(String tenantId) {
        if (tenantId != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SCHEDULE_UPDATED");
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
        }
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month, String branchId) {
        if (branchId == null || branchId.isEmpty() || "null".equals(branchId)) {
            return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
        } else {
            return appointmentRepository.getWorkloadForMonthAndBranch(tenantId, year, month, branchId);
        }
    }
}
