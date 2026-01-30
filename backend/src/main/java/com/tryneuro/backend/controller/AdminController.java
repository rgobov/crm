package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.ContactService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService;
    private final ContactService contactService;

    @Autowired
    public AdminController(StaffMemberService staffMemberService,
                           ScheduleService scheduleService,
                           ContactService contactService) {
        this.staffMemberService = staffMemberService;
        this.scheduleService = scheduleService;
        this.contactService = contactService;
    }

    // --- Staff Management ---
    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return staffMemberService.getStaffPaged(tenantId, query, page, size);
    }

    @GetMapping("/staff/{id}")
    public StaffMember getStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        StaffMember staff = staffMemberService.getStaffMemberById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сотрудник не найден"));

        if (!staff.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещен");
        }
        return staff;
    }

    // --- Client Management for Admin ---
    @GetMapping("/clients/{contactId}/appointments")
    public List<Appointment> getClientAppointmentsAsAdmin(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String contactId) {
        return scheduleService.getAppointmentsForContact(contactId, tenantId);
    }

    @PutMapping("/clients/{id}")
    public Contact updateClientAsAdmin(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String id,
            @RequestBody Contact contact) {
        // Гарантируем безопасность через Service
        return contactService.updateContact(id, contact, tenantId);
    }

    @PostMapping("/staff")
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, tenantId);
    }
    
    @PutMapping("/staff/{id}")
    public StaffMember updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return staffMemberService.updateStaffMember(id, request, tenantId);
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }

    @GetMapping("/staff/{staffMemberId}/availability")
    public boolean isStaffMemberAvailable(@RequestAttribute("tenantId") String tenantId,
                                            @PathVariable String staffMemberId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                                            @RequestParam int duration,
                                            @RequestParam(required = false) String currentAppointmentId) {
        return scheduleService.isStaffMemberAvailable(tenantId, staffMemberId, date, time, duration, currentAppointmentId);
    }
}
