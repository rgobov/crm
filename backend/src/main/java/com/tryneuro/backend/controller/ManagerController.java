package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService;

    @Autowired
    public ManagerController(StaffMemberService staffMemberService, ScheduleService scheduleService) {
        this.staffMemberService = staffMemberService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(@RequestAttribute("tenantId") String tenantId, 
                                                   @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForDay(date, tenantId);
    }

    // --- НОВЫЙ ЭНДПОИНТ ДЛЯ КАЛЕНДАРЯ ---
    @GetMapping("/workload")
    public List<WorkloadDto> getWorkload(@RequestAttribute("tenantId") String tenantId, 
                                       @RequestParam int year, 
                                       @RequestParam int month) {
        return scheduleService.getWorkloadForMonth(tenantId, year, month);
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(@RequestAttribute("tenantId") String tenantId) {
        List<StaffMember> allStaff = staffMemberService.getAllStaff(tenantId);
        return allStaff.stream()
                .filter(staff -> "EMPLOYEE".equals(staff.getRole()))
                .collect(Collectors.toList());
    }

    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Appointment appointment, @RequestAttribute("tenantId") String tenantId) {
        appointment.setTenantId(tenantId);
        return scheduleService.addAppointment(appointment);
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
