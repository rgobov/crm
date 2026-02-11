package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.StaffMember;
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
    private final StaffMemberRepository staffMemberRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ScheduleService(AppointmentRepository appointmentRepository, 
                           StaffMemberRepository staffMemberRepository,
                           StaffShiftRepository staffShiftRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Appointment> getAppointmentsForDay(LocalDate date, String tenantId) {
        return appointmentRepository.findByDateAndTenantId(date, tenantId);
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        return appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
    }

    // ВОССТАНОВЛЕННЫЙ МЕТОД: Для ContactController (Flutter версия)
    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        return appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public Appointment addAppointment(Appointment appointment) {
        log.info("📝 Creating new appointment for client: {}", appointment.getClientName());
        validateAvailability(appointment);
        Appointment saved = appointmentRepository.save(appointment);
        notifyChange(saved.getTenantId());
        return saved;
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details) {
        log.info("♻️ Re-creating appointment (Delete & Create) for id: {}", id);

        Appointment oldAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));

        String tenantId = oldOldAppointmentTenantId(oldAppointment);

        appointmentRepository.delete(oldAppointment);
        
        details.setId(null);
        details.setTenantId(tenantId);

        validateAvailability(details);
        Appointment saved = appointmentRepository.save(details);
        
        notifyChange(tenantId);
        return saved;
    }

    private String oldOldAppointmentTenantId(Appointment old) {
        return old.getTenantId();
    }

    @Transactional
    public void deleteAppointment(String id) {
        log.info("🗑 Deleting appointment: {}", id);
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
        if (app.getStaffMemberId() != null) {
            if (!isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), app.getDate(), app.getTime(), app.getDurationInMinutes(), null)) {
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

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month) {
        return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
    }
}
