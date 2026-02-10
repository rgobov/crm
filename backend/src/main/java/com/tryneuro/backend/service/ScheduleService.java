package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ScheduleService {
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

    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        return appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month) {
        return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
    }

    public boolean isStaffMemberAvailable(String tenantId, String staffMemberId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId).orElse(null);
        if (staffMember == null || !staffMember.isActive()) return false;

        Optional<StaffShift> shiftOpt = staffShiftRepository.findByStaffIdAndDate(staffMemberId, date);
        if (shiftOpt.isEmpty() || shiftOpt.get().isDayOff()) {
            return false;
        }

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

    public boolean isResourceAvailable(String resourceId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        if (resourceId == null) return true;
        List<Appointment> resourceApps = appointmentRepository.findByResourceIdAndDate(resourceId, date);
        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        for (Appointment existing : resourceApps) {
            if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;

            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }
    
    public Appointment addAppointment(Appointment appointment) {
        validateAvailability(appointment);
        Appointment saved = appointmentRepository.save(appointment);
        notifyChange(saved.getTenantId());
        return saved;
    }

    public Appointment updateAppointment(String id, Appointment details) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));

        appointment.setDate(details.getDate());
        appointment.setTime(details.getTime());
        appointment.setDurationInMinutes(details.getDurationInMinutes());
        appointment.setClientName(details.getClientName());
        appointment.setContactId(details.getContactId());
        appointment.setService(details.getService());
        appointment.setStaffMemberId(details.getStaffMemberId());
        appointment.setResourceId(details.getResourceId());
        appointment.setStatus(details.getStatus());
        appointment.setComment(details.getComment());
        
        Appointment updated = appointmentRepository.save(appointment);
        notifyChange(updated.getTenantId());
        return updated;
    }

    public void deleteAppointment(String id) {
        appointmentRepository.findById(id).ifPresent(app -> {
            String tenantId = app.getTenantId();
            appointmentRepository.deleteById(id);
            notifyChange(tenantId);
        });
    }

    /**
     * Профессиональное уведомление через WebSocket.
     * Шлем объект с меткой времени, чтобы клиент всегда видел изменение состояния.
     */
    private void notifyChange(String tenantId) {
        if (tenantId != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SCHEDULE_REFRESH");
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
}
