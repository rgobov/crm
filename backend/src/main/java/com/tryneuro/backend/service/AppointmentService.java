package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<Appointment> getAllAppointments(String tenantId) {
        return appointmentRepository.findByTenantId(tenantId);
    }
    
    public List<Appointment> getAppointmentsForDay(String tenantId, LocalDate date) {
        return appointmentRepository.findByTenantIdAndDate(tenantId, date);
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        return appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
    }

    public Appointment addAppointment(Appointment appointment, String tenantId) {
        appointment.setTenantId(tenantId);
        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointment(String id, Appointment appointment, String tenantId) {
        appointment.setId(id);
        appointment.setTenantId(tenantId);
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(String id) {
        appointmentRepository.deleteById(id);
    }
}
