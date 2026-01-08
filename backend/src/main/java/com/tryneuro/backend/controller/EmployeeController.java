package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final ScheduleService scheduleService;
    private final StaffMemberService staffMemberService;

    @Autowired
    public EmployeeController(ScheduleService scheduleService, StaffMemberService staffMemberService) {
        this.scheduleService = scheduleService;
        this.staffMemberService = staffMemberService;
    }

    @GetMapping("/profile")
    public ResponseEntity<StaffMember> getMyProfile(@AuthenticationPrincipal User user) {
        if (user.getStaffId() == null) {
            return ResponseEntity.notFound().build();
        }
        return staffMemberService.getStaffMemberById(user.getStaffId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointments")
    public List<Appointment> getMyAppointmentsForDay(@AuthenticationPrincipal User user, @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (user.getStaffId() == null) {
            return Collections.emptyList();
        }
        return scheduleService.getAppointmentsForStaff(user.getTenantId(), user.getStaffId(), date);
    }

    @GetMapping("/workload")
    public List<WorkloadDto> getMyWorkload(@AuthenticationPrincipal User user, @RequestParam int year, @RequestParam int month) {
        if (user.getStaffId() == null) {
            return Collections.emptyList();
        }
        return scheduleService.getWorkloadForStaffAndMonth(user.getStaffId(), year, month);
    }
}
