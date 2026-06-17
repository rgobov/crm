package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.CommentRequest;
import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentComment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.service.CommentService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Point of Truth for Svelte Employee (Master) Panel
 */
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final ScheduleService scheduleService;
    private final StaffMemberService staffMemberService;
    private final CommentService commentService;

    @Autowired
    public EmployeeController(ScheduleService scheduleService, StaffMemberService staffMemberService, CommentService commentService) {
        this.scheduleService = scheduleService;
        this.staffMemberService = staffMemberService;
        this.commentService = commentService;
    }

    private String getRequiredStaffId(User user) {
        if (user.getStaffId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ошибка: ваш аккаунт не связан с профилем сотрудника");
        }
        return user.getStaffId();
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> getMyDashboardStats(@AuthenticationPrincipal User user) {
        String sId = getRequiredStaffId(user);
        Map<String, Object> stats = new HashMap<>();
        
        List<Appointment> todayApps = scheduleService.getAppointmentsForStaff(user.getTenantId(), sId, LocalDate.now());
        List<WorkloadDto> workload = scheduleService.getWorkloadForStaffAndMonth(sId, LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        
        stats.put("todayAppointmentsCount", todayApps.size());
        stats.put("monthlyWorkload", workload);
        stats.put("staffName", user.getUsername());
        
        return stats;
    }

    @GetMapping("/profile")
    public ResponseEntity<StaffMember> getMyProfile(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String branchId) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return staffMemberService.getStaffByIdAndDate(getRequiredStaffId(user), targetDate, branchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/shift")
    public ResponseEntity<StaffShift> updateMyShift(@AuthenticationPrincipal User user, @RequestBody StaffShift shift) {
        shift.setStaffId(getRequiredStaffId(user));
        shift.setTenantId(user.getTenantId());
        return ResponseEntity.ok(staffMemberService.saveShift(shift));
    }

    @PostMapping("/profile/shift/copy")
    public ResponseEntity<Void> copyShift(@AuthenticationPrincipal User user, @RequestBody StaffShift sourceShift, @RequestParam int days) {
        String sId = getRequiredStaffId(user);
        for (int i = 1; i <= days; i++) {
            StaffShift newShift = new StaffShift();
            newShift.setStaffId(sId);
            newShift.setTenantId(user.getTenantId());
            newShift.setDate(sourceShift.getDate().plusDays(i));
            newShift.setWorkStartTime(sourceShift.getWorkStartTime());
            newShift.setWorkEndTime(sourceShift.getWorkEndTime());
            newShift.setBreakStartTime(sourceShift.getBreakStartTime());
            newShift.setBreakEndTime(sourceShift.getBreakEndTime());
            newShift.setDayOff(sourceShift.isDayOff());
            staffMemberService.saveShift(newShift);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments")
    public List<Appointment> getMyAppointmentsForDay(@AuthenticationPrincipal User user, @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForStaff(user.getTenantId(), getRequiredStaffId(user), date);
    }

    @GetMapping("/appointments/{id}/comments")
    public List<AppointmentComment> getComments(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return commentService.getCommentsForAppointment(tenantId, id);
    }

    @PostMapping("/appointments/{id}/comments")
    public AppointmentComment addComment(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal User user) {
        return commentService.addCommentToAppointment(tenantId, id, request.getText(), user);
    }
}
