package com.tryneuro.backend.controller;

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
import java.time.LocalTime;
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

    private String validateTenant(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            log.error("CORS/Security: tenantId is MISSING in request attributes!");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ошибка авторизации: не удалось определить ID компании");
        }
        return tenantId;
    }

    // --- DASHBOARD ---
    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats(@RequestAttribute("tenantId") String tenantId) {
        String tId = validateTenant(tenantId);
        log.info("Fetching dashboard stats for tenant: {}", tId);
        
        Map<String, Object> stats = new HashMap<>();
        long clients = contactService.countContacts(tId);
        long staff = staffMemberService.getStaffPaged(tId, null, 0, 1).getTotalElements();
        int resources = resourceService.getAllResources(tId).size();
        
        log.info("Stats result for {}: Clients={}, Staff={}, Resources={}", tId, clients, staff, resources);
        
        stats.put("totalClients", clients);
        stats.put("totalStaff", staff);
        stats.put("totalResources", resources);
        stats.put("todayAppointments", scheduleService.getAppointmentsForDay(LocalDate.now(), tId).size());
        return stats;
    }

    // --- CLIENTS ---
    @GetMapping("/clients")
    public Page<Contact> getClientsPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String tId = validateTenant(tenantId);
        log.info("Fetching clients for tenant: {}, query: {}", tId, query);
        return contactService.getContactsPaged(tId, query, false, page, size);
    }

    // --- STAFF ---
    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String tId = validateTenant(tenantId);
        log.info("Fetching staff for tenant: {}", tId);
        return staffMemberService.getStaffPaged(tId, query, page, size);
    }

    @PostMapping("/staff")
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, validateTenant(tenantId));
    }

    @PutMapping("/staff/{id}")
    public StaffMember updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return staffMemberService.updateStaffMember(id, request, validateTenant(tenantId));
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }

    // --- CALENDAR & SCHEDULE ---
    @GetMapping("/workload")
    public List<WorkloadDto> getWorkload(@RequestAttribute("tenantId") String tenantId,
                                         @RequestParam int year,
                                         @RequestParam int month) {
        return scheduleService.getWorkloadForMonth(validateTenant(tenantId), year, month);
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(@RequestAttribute("tenantId") String tenantId,
                                                   @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForDay(date, validateTenant(tenantId));
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffMemberService.getStaffForDate(validateTenant(tenantId), date);
    }

    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Appointment appointment, @RequestAttribute("tenantId") String tenantId) {
        appointment.setTenantId(validateTenant(tenantId));
        return scheduleService.addAppointment(appointment);
    }

    // --- RESOURCES & SERVICES ---
    @GetMapping("/resources")
    public List<Resource> getAllResources(@RequestAttribute("tenantId") String tenantId) {
        return resourceService.getAllResources(validateTenant(tenantId));
    }

    @GetMapping("/services")
    public List<com.tryneuro.backend.model.Service> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(validateTenant(tenantId));
    }
}
