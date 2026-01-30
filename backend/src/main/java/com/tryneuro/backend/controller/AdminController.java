package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.AppServiceService;
import com.tryneuro.backend.service.ContactService;
import com.tryneuro.backend.service.ResourceService;
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
    private final AppServiceService appServiceService;
    private final ResourceService resourceService;

    @Autowired
    public AdminController(StaffMemberService staffMemberService, 
                           ScheduleService scheduleService,
                           ContactService contactService,
                           AppServiceService appServiceService,
                           ResourceService resourceService) {
        this.staffMemberService = staffMemberService;
        this.scheduleService = scheduleService;
        this.contactService = contactService;
        this.appServiceService = appServiceService;
        this.resourceService = resourceService;
    }

    // --- Управление персоналом (Staff) ---
    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return staffMemberService.getStaffPaged(tenantId, query, page, size);
    }

    @PostMapping("/staff")
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, tenantId);
    }

    // --- Управление услугами (Services) ---
    @GetMapping("/services")
    public List<com.tryneuro.backend.model.Service> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(tenantId);
    }

    @PostMapping("/services")
    public com.tryneuro.backend.model.Service addService(@RequestAttribute("tenantId") String tenantId, 
                                                         @RequestBody com.tryneuro.backend.model.Service service) {
        return appServiceService.addService(service, tenantId);
    }

    @DeleteMapping("/services/{id}")
    public void deleteService(@PathVariable String id) {
        appServiceService.deleteService(id);
    }

    // --- Управление ресурсами (Resources) ---
    @GetMapping("/resources")
    public List<Resource> getAllResources(@RequestAttribute("tenantId") String tenantId) {
        return resourceService.getAllResources(tenantId);
    }

    @PostMapping("/resources")
    public Resource addResource(@RequestAttribute("tenantId") String tenantId, @RequestBody Resource resource) {
        return resourceService.addResource(resource, tenantId);
    }

    @DeleteMapping("/resources/{id}")
    public void deleteResource(@PathVariable String id) {
        resourceService.deleteResource(id);
    }

    // --- Управление клиентами (Clients) ---
    @PutMapping("/clients/{id}")
    public Contact updateClientAsAdmin(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String id,
            @RequestBody Contact contact) {
        return contactService.updateContact(id, contact, tenantId);
    }

    @GetMapping("/clients/{contactId}/appointments")
    public List<Appointment> getClientAppointmentsAsAdmin(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String contactId) {
        return scheduleService.getAppointmentsForContact(contactId, tenantId);
    }

    // --- Доступность мастеров ---
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
