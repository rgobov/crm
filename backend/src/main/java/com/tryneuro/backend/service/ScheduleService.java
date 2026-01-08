package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month) {
        return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
    }

    // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Добавляем tenantId ---
    public boolean isStaffMemberAvailable(String tenantId, String staffMemberId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId).orElse(null);
        if (staffMember == null || !staffMember.isAvailable()) {
            return false;
        }

        LocalTime appointmentStartTime = time;
        LocalTime appointmentEndTime = time.plusMinutes(duration);

        if (staffMember.getWorkStartTime() != null && staffMember.getWorkEndTime() != null) {
            if (appointmentStartTime.isBefore(staffMember.getWorkStartTime())) {
                return false;
            }
            if (appointmentEndTime.isAfter(staffMember.getWorkEndTime())) {
                return false;
            }
            if (staffMember.getBreakStartTime() != null && staffMember.getBreakEndTime() != null) {
                if (appointmentStartTime.isBefore(staffMember.getBreakEndTime()) && appointmentEndTime.isAfter(staffMember.getBreakStartTime())) {
                    return false;
                }
            }
        }

        // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Используем правильный метод репозитория ---
        List<Appointment> staffAppointments = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffMemberId, date);
        for (Appointment existingAppointment : staffAppointments) {
            if (currentAppointmentId != null && existingAppointment.getId().equals(currentAppointmentId)) {
                continue;
            }
            LocalTime existingStart = existingAppointment.getTime();
            LocalTime existingEnd = existingStart.plusMinutes(existingAppointment.getDurationInMinutes());
            if (appointmentStartTime.isBefore(existingEnd) && appointmentEndTime.isAfter(existingStart)) {
                return false;
            }
        }

        return true;
    }

    public boolean isResourceAvailable(String resourceId, LocalDate date, LocalTime time, int duration, String currentAppointmentId) {
        List<Appointment> resourceAppointments = appointmentRepository.findByResourceIdAndDate(resourceId, date);
        LocalTime newAppointmentStart = time;
        LocalTime newAppointmentEnd = time.plusMinutes(duration);

        for (Appointment existingAppointment : resourceAppointments) {
            if (currentAppointmentId != null && existingAppointment.getId().equals(currentAppointmentId)) {
                continue;
            }
            LocalTime existingStart = existingAppointment.getTime();
            LocalTime existingEnd = existingStart.plusMinutes(existingAppointment.getDurationInMinutes());
            if (newAppointmentStart.isBefore(existingEnd) && newAppointmentEnd.isAfter(existingStart)) {
                return false;
            }
        }
        return true;
    }
    
    public Appointment addAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointment(String id, Appointment appointmentDetails) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setDate(appointmentDetails.getDate());
        appointment.setTime(appointmentDetails.getTime());
        appointment.setDurationInMinutes(appointmentDetails.getDurationInMinutes());
        appointment.setClientName(appointmentDetails.getClientName());
        appointment.setService(appointmentDetails.getService());
        appointment.setStaffMemberId(appointmentDetails.getStaffMemberId());
        appointment.setResourceId(appointmentDetails.getResourceId());
        appointment.setStatus(appointmentDetails.getStatus());
        appointment.setComment(appointmentDetails.getComment());
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(String id) {
        appointmentRepository.deleteById(id);
    }
}
