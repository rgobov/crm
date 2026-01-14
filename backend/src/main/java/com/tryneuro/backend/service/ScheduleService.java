package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleService {
    private final AppointmentRepository appointmentRepository;
    private final StaffMemberRepository staffMemberRepository;

    @Autowired
    public ScheduleService(AppointmentRepository appointmentRepository, StaffMemberRepository staffMemberRepository) {
        this.appointmentRepository = appointmentRepository;
        this.staffMemberRepository = staffMemberRepository;
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
        if (staffMember == null || !staffMember.isAvailable()) {
            return false;
        }

        LocalTime start = time;
        LocalTime end = time.plusMinutes(duration);

        // 1. Проверка рабочего графика
        if (staffMember.getWorkStartTime() != null && staffMember.getWorkEndTime() != null) {
            if (start.isBefore(staffMember.getWorkStartTime()) || end.isAfter(staffMember.getWorkEndTime())) {
                return false;
            }
            if (staffMember.getBreakStartTime() != null && staffMember.getBreakEndTime() != null) {
                if (start.isBefore(staffMember.getBreakEndTime()) && end.isAfter(staffMember.getBreakStartTime())) {
                    return false;
                }
            }
        }

        // 2. Проверка пересечений с другими записями
        List<Appointment> staffAppointments = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffMemberId, date);
        for (Appointment existing : staffAppointments) {
            if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) continue;
            
            LocalTime eStart = existing.getTime();
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            if (start.isBefore(eEnd) && end.isAfter(eStart)) {
                return false;
            }
        }
        return true;
    }

    public boolean isResourceAvailable(String resourceId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        if (resourceId == null) return true;
        
        List<Appointment> resourceApps = appointmentRepository.findByResourceIdAndDate(resourceId, date);
        LocalTime start = time;
        LocalTime end = time.plusMinutes(duration);

        for (Appointment existing : resourceApps) {
            if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) continue;
            
            LocalTime eStart = existing.getTime();
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            if (start.isBefore(eEnd) && end.isAfter(eStart)) {
                return false;
            }
        }
        return true;
    }
    
    public Appointment addAppointment(Appointment appointment) {
        // --- ЗАЩИТА: ПРОВЕРКА ПЕРЕД СОХРАНЕНИЕМ ---
        validateAvailability(appointment);
        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointment(String id, Appointment details) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        // --- ЗАЩИТА: ПРОВЕРКА ПЕРЕД ОБНОВЛЕНИЕМ ---
        validateAvailability(details);

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
        
        return appointmentRepository.save(appointment);
    }

    private void validateAvailability(Appointment app) {
        if (app.getStaffMemberId() != null) {
            if (!isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), app.getDate(), app.getTime(), app.getDurationInMinutes(), app.getId().equals("new") ? null : app.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Сотрудник занят в это время");
            }
        }
        if (app.getResourceId() != null) {
            if (!isResourceAvailable(app.getResourceId(), app.getDate(), app.getTime(), app.getDurationInMinutes(), app.getId().equals("new") ? null : app.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс (кабинет) занят в это время");
            }
        }
    }

    public Appointment updateAppointment(Appointment appointmentDetails) {
        return updateAppointment(appointmentDetails.getId(), appointmentDetails);
    }

    public void deleteAppointment(String id) {
        appointmentRepository.deleteById(id);
    }
}
