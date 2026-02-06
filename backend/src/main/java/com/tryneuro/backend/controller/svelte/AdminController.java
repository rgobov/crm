package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.AppServiceService;
import com.tryneuro.backend.service.ContactService;
import com.tryneuro.backend.service.ResourceService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ошибка авторизации: не удалось определить ID компании");
        }
        return tenantId;
    }

    // --- DASHBOARD ---
    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats(@RequestAttribute("tenantId") String tenantId) {
        String tId = getRequiredTenantId(tenantId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", contactService.countContacts(tId));
        stats.put("totalStaff", staffMemberService.getAllStaff(tId).size());
        stats.put("totalResources", resourceService.getAllResources(tId).size());
        stats.put("todayAppointments", scheduleService.getAppointmentsForDay(LocalDate.now(), tId).size());
        return stats;
    }

    // --- STAFF ---
    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return staffMemberService.getStaffPaged(getRequiredTenantId(tenantId), query, active, page, size);
    }

    @PostMapping("/staff")
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, getRequiredTenantId(tenantId));
    }

    @PutMapping("/staff/{id}")
    public StaffMember updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return staffMemberService.updateStaffMember(id, request, getRequiredTenantId(tenantId));
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }

    // --- CLIENTS ---
    @GetMapping("/clients")
    public Page<Contact> getClientsPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") boolean showAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return contactService.getContactsPaged(getRequiredTenantId(tenantId), query, showAll, page, size);
    }

    // --- SCHEDULE ---
    @GetMapping("/workload")
    public List<WorkloadDto> getWorkload(@RequestAttribute("tenantId") String tenantId, @RequestParam int year, @RequestParam int month) {
        return scheduleService.getWorkloadForMonth(getRequiredTenantId(tenantId), year, month);
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(@RequestAttribute("tenantId") String tenantId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForDay(date, getRequiredTenantId(tenantId));
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(@RequestAttribute("tenantId") String tenantId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffMemberService.getStaffForDate(getRequiredTenantId(tenantId), date);
    }

    // --- RESOURCES & SERVICES ---
    @GetMapping("/resources")
    public List<Resource> getAllResources(@RequestAttribute("tenantId") String tenantId) {
        return resourceService.getAllResources(getRequiredTenantId(tenantId));
    }

    @GetMapping("/services")
    public List<com.tryneuro.backend.model.Service> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(getRequiredTenantId(tenantId));
    }
}
