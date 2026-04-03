package com.tryneuro.backend.controller.svelte;



import com.tryneuro.backend.dto.*;

import com.tryneuro.backend.model.Appointment;

import com.tryneuro.backend.model.WappiSettings;

import com.tryneuro.backend.service.ContactService;

import com.tryneuro.backend.service.ScheduleService;

import com.tryneuro.backend.service.StaffMemberService;

import com.tryneuro.backend.service.WappiService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;



import java.time.LocalDate;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



@RestController

@RequestMapping("/api/manager")

public class ManagerController {



    private final StaffMemberService staffMemberService;

    private final ScheduleService scheduleService;

    private final WappiService wappiService;

    private final ContactService contactService;



    @Autowired

    public ManagerController(StaffMemberService staffMemberService, 

                             ScheduleService scheduleService, 

                             WappiService wappiService,

                             ContactService contactService) {

        this.staffMemberService = staffMemberService;

        this.scheduleService = scheduleService;

        this.wappiService = wappiService;

        this.contactService = contactService;

    }



    private String getRequiredTenantId(String tenantId) {

        if (tenantId == null || tenantId.isEmpty()) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ошибка авторизации: ID компании не определен");

        }

        return tenantId;

    }



    @GetMapping("/dashboard/stats")

    public Map<String, Object> getDashboardStats(@RequestAttribute("tenantId") String tenantId) {

        String tId = getRequiredTenantId(tenantId);

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalClients", contactService.countContacts(tId));

        stats.put("totalStaff", staffMemberService.getAllStaff(tId).size());

        stats.put("todayAppointments", scheduleService.getAppointmentsForDay(LocalDate.now(), tId, null).size());

        return stats;

    }



    @GetMapping("/appointments/day")

    public List<AppointmentDto> getAppointmentsForDay(

            @RequestAttribute("tenantId") String tenantId, 

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @RequestParam(required = false) String branchId) {

        return scheduleService.getAppointmentsForDay(date, getRequiredTenantId(tenantId), branchId)

                .stream().map(DtoMapper::toDto).collect(Collectors.toList());

    }



    @GetMapping("/workload")

    public List<WorkloadDto> getWorkload(

            @RequestAttribute("tenantId") String tenantId, 

            @RequestParam int year, 

            @RequestParam int month,

            @RequestParam(required = false) String branchId) {

        return scheduleService.getWorkloadForMonth(getRequiredTenantId(tenantId), year, month, branchId);

    }



    @GetMapping("/schedule/staff")

    public List<StaffMemberDto> getStaffForSchedule(

            @RequestAttribute("tenantId") String tenantId, 

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @RequestParam(required = false) String branchId) {

        return staffMemberService.getStaffForDate(getRequiredTenantId(tenantId), date, branchId)

                .stream().map(DtoMapper::toDto).collect(Collectors.toList());

    }



    // Wappi Settings

    @GetMapping("/settings/wappi")

    public WappiSettings getWappiSettings(@RequestAttribute("tenantId") String tenantId) {

        return wappiService.getSettings(getRequiredTenantId(tenantId));

    }



    @PutMapping("/settings/wappi")

    public WappiSettings updateWappiSettings(@RequestAttribute("tenantId") String tenantId, @RequestBody WappiSettings settings) {

        return wappiService.saveSettings(getRequiredTenantId(tenantId), settings);

    }

}

