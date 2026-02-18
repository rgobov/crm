package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ScheduleService(AppointmentRepository appointmentRepository, 
                           StaffShiftRepository staffShiftRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Appointment> getAppointmentsForDay(LocalDate date, String tenantId, String branchId) {
        return appointmentRepository.findByDateAndTenantIdAndBranchId(date, tenantId, branchId);
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        return appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
    }

    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        return appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    @Transactional
    public Appointment addAppointment(Appointment appointment) {
        log.info("--- Appointment Creation DEBUG ---");
        log.info("Received appointment for client: {}", appointment.getClientName());
        
        // Проверяем, какой branchId пришел от фронтенда
        if (appointment.getBranch() != null && appointment.getBranch().getId() != null) {
            log.info("Branch object is present. ID: {}", appointment.getBranch().getId());
            appointment.setBranchId(appointment.getBranch().getId());
        } else {
            log.warn("Branch object is NULL or has no ID.");
        }

        log.info("Final branchId before save: {}", appointment.getBranchId());

        validateAvailability(appointment);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Saved appointment ID: {}. Branch ID in saved object: {}", saved.getId(), saved.getBranchId());
        log.info("-----------------------------------");
        notifyChange(saved.getTenantId());
        return saved;
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details) {
        // ... (логика обновления)
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
        if (details.getBranch() != null && details.getBranch().getId() != null) {
            appointment.setBranchId(details.getBranch().getId());
        }
        validateAvailability(appointment);
        Appointment updated = appointmentRepository.save(appointment);
        notifyChange(updated.getTenantId());
        return updated;
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

    private void validateAvailability(Appointment app) {
        String appId = (app.getId() == null || app.getId().equals("new")) ? null : app.getId();
        if (app.getStaffMemberId() != null) {
            if (!isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), app.getDate(), app.getTime(), app.getDurationInMinutes(), appId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Мастер занят на это время");
            }
        }
    }

    public boolean isStaffMemberAvailable(String tenantId, String staffMemberId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        Optional<StaffShift> shiftOpt = staffShiftRepository.findByStaffIdAndDate(staffMemberId, date);
        if (shiftOpt.isEmpty() || shiftOpt.get().isDayOff()) return false;

        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);

        List<Appointment> staffAppointments = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffMemberId, date);
        for (Appointment existing : staffAppointments) {
            if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month, String branchId) {
        log.info("📊 Workload Request: tenant={}, branch={}, year={}, month={}", tenantId, branchId, year, month);
        if (branchId == null || branchId.isEmpty() || "null".equals(branchId)) {
            log.info("🔍 Calling GLOBAL workload query");
            return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
        } else {
            log.info("🔍 Calling BRANCH-SPECIFIC workload query for: {}", branchId);
            return appointmentRepository.getWorkloadForMonthAndBranch(tenantId, year, month, branchId);
        }
    }
}
