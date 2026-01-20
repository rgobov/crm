package com.tryneuro.backend.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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

    @GetMapping("/profile")
    public ResponseEntity<StaffMember> getMyProfile(@AuthenticationPrincipal User user, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (user.getStaffId() == null) {
            return ResponseEntity.notFound().build();
        }
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return staffMemberService.getStaffByIdAndDate(user.getStaffId(), targetDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/shift")
    public ResponseEntity<StaffShift> updateMyShift(@AuthenticationPrincipal User user, @RequestBody StaffShift shift) {
        if (user.getStaffId() == null) {
            return ResponseEntity.status(403).build();
        }
        shift.setStaffId(user.getStaffId());
        shift.setTenantId(user.getTenantId());
        StaffShift saved = staffMemberService.saveShift(shift);
        return ResponseEntity.ok(saved);
    }

    // --- НОВЫЙ ЭНДПОИНТ: Копирование графика на период ---
    @PostMapping("/profile/shift/copy")
    public ResponseEntity<Void> copyShift(@AuthenticationPrincipal User user,
                                          @RequestBody StaffShift sourceShift,
                                          @RequestParam int days) {
        if (user.getStaffId() == null) return ResponseEntity.status(403).build();

        for (int i = 1; i <= days; i++) {
            StaffShift newShift = new StaffShift();
            newShift.setStaffId(user.getStaffId());
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
        if (user.getStaffId() == null) {
            return Collections.emptyList();
        }
        return scheduleService.getAppointmentsForStaff(user.getTenantId(), user.getStaffId(), date);
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable String id, @RequestBody Appointment appointmentDetails) {
        Appointment updatedAppointment = scheduleService.updateAppointment(id, appointmentDetails);
        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id) {
        scheduleService.deleteAppointment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workload")
    public List<WorkloadDto> getMyWorkload(@AuthenticationPrincipal User user, @RequestParam int year, @RequestParam int month) {
        if (user.getStaffId() == null) {
            return Collections.emptyList();
        }
        return scheduleService.getWorkloadForStaffAndMonth(user.getStaffId(), year, month);
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
