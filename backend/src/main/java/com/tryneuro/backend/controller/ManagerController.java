package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CommentRequest;
import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentComment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.service.CommentService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import com.tryneuro.backend.service.WappiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final CommentService commentService;
    private final WappiService wappiService;

    @Autowired
    public ManagerController(StaffMemberService staffMemberService, 
                             ScheduleService scheduleService, 
                             CommentService commentService,
                             WappiService wappiService) {
        this.staffMemberService = staffMemberService;
        this.scheduleService = scheduleService;
        this.commentService = commentService;
        this.wappiService = wappiService;
    }

    @GetMapping("/settings/wappi")
    public WappiSettings getWappiSettings(@RequestAttribute("tenantId") String tenantId) {
        return wappiService.getSettings(tenantId);
    }

    @PutMapping("/settings/wappi")
    public WappiSettings updateWappiSettings(@RequestAttribute("tenantId") String tenantId, 
                                             @RequestBody WappiSettings settings) {
        return wappiService.saveSettings(tenantId, settings);
    }

    @PostMapping("/settings/wappi/test")
    public ResponseEntity<Void> sendTestReminder(@RequestAttribute("tenantId") String tenantId, 
                                                 @RequestParam String phone) {
        wappiService.sendTestMessage(tenantId, phone);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(@RequestAttribute("tenantId") String tenantId, @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getAppointmentsForDay(date, tenantId);
    }

    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Appointment appointment, @RequestAttribute("tenantId") String tenantId) {
        appointment.setTenantId(tenantId);
        return scheduleService.addAppointment(appointment);
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
    public List<WorkloadDto> getWorkload(@RequestAttribute("tenantId") String tenantId, @RequestParam int year, @RequestParam int month) {
        return scheduleService.getWorkloadForMonth(tenantId, year, month);
    }

    // Обновлено: теперь принимает дату для получения актуального графика смен
    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffMemberService.getStaffForDate(tenantId, date);
    }

    @GetMapping("/staff/{staffMemberId}/availability")
    public boolean isStaffMemberAvailable(@RequestAttribute("tenantId") String tenantId, @PathVariable String staffMemberId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time, @RequestParam int duration, @RequestParam(required = false) String currentAppointmentId) {
        return scheduleService.isStaffMemberAvailable(tenantId, staffMemberId, date, time, duration, currentAppointmentId);
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
