package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService; // Добавляем сервис расписания

    @Autowired
    public ManagerController(StaffMemberService staffMemberService, ScheduleService scheduleService) {
        this.staffMemberService = staffMemberService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(@RequestAttribute("tenantId") String tenantId) {
        List<StaffMember> allStaff = staffMemberService.getAllStaff(tenantId);

        return allStaff.stream()
                .filter(staff -> "EMPLOYEE".equals(staff.getRole()))
                .collect(Collectors.toList());
    }

    // --- НОВЫЙ МЕТОД ДЛЯ СОЗДАНИЯ ЗАПИСИ ---
    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Appointment appointment, @RequestAttribute("tenantId") String tenantId) {
        appointment.setTenantId(tenantId); // Устанавливаем ID компании
        return scheduleService.addAppointment(appointment);
    }
}
