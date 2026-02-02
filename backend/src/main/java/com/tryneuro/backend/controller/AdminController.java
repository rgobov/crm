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
            log.error("Security Alert: Missing tenantId in request!");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        return tenantId;
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats(@RequestAttribute("tenantId") String tenantId) {
        String tId = validateTenant(tenantId);
        log.info("API: Fetching stats for {}", tId);
        
        Map<String, Object> stats = new HashMap<>();
        // Используем те же ключи, что и во фронтенде
        stats.put("totalClients", contactService.countContacts(tId));
        stats.put("totalStaff", staffMemberService.getStaffPaged(tId, null, 0, 1).getTotalElements());
        stats.put("totalResources", resourceService.getAllResources(tId).size());
        stats.put("todayAppointments", scheduleService.getAppointmentsForDay(LocalDate.now(), tId).size());
        
        log.info("API: Stats ready -> {}", stats);
        return stats;
    }

    @GetMapping("/clients")
    public Page<Contact> getClientsPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String tId = validateTenant(tenantId);
        log.info("API: Fetching clients for {}, page: {}", tId, page);
        return contactService.getContactsPaged(tId, query, false, page, size);
    }

    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String tId = validateTenant(tenantId);
        log.info("API: Fetching staff for {}", tId);
        return staffMemberService.getStaffPaged(tId, query, page, size);
    }

    @GetMapping("/resources")
    public List<Resource> getAllResources(@RequestAttribute("tenantId") String tenantId) {
        return resourceService.getAllResources(validateTenant(tenantId));
    }

    @GetMapping("/workload")
    public List<WorkloadDto> getWorkload(@RequestAttribute("tenantId") String tenantId, @RequestParam int year, @RequestParam int month) {
        return scheduleService.getWorkloadForMonth(validateTenant(tenantId), year, month);
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(@RequestAttribute("tenantId") String tenantId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForDay(date, validateTenant(tenantId));
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(@RequestAttribute("tenantId") String tenantId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffMemberService.getStaffForDate(validateTenant(tenantId), date);
    }
}
