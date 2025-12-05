package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> getAllAppointments(@RequestHeader("X-Tenant-ID") String tenantId) {
        return appointmentService.getAllAppointments(tenantId);
    }
    
    @GetMapping("/day")
    public List<Appointment> getAppointmentsForDay(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.getAppointmentsForDay(tenantId, date);
    }

    @GetMapping("/staff/{staffId}")
    public List<Appointment> getAppointmentsForStaff(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String staffId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.getAppointmentsForStaff(tenantId, staffId, date);
    }

    @PostMapping
    public Appointment createAppointment(@RequestHeader("X-Tenant-ID") String tenantId, @RequestBody Appointment appointment) {
        return appointmentService.addAppointment(appointment, tenantId);
    }

    @PutMapping("/{id}")
    public Appointment updateAppointment(@RequestHeader("X-Tenant-ID") String tenantId, @PathVariable String id, @RequestBody Appointment appointment) {
        return appointmentService.updateAppointment(id, appointment, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable String id) {
        appointmentService.deleteAppointment(id);
    }
}
