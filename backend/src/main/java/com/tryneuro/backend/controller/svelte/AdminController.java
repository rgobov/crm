package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService;
    private final ContactService contactService;
    private final AppServiceService appServiceService;
    private final ResourceService resourceService;
    private final DashboardService dashboardService;

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        return tenantId;
    }

    @GetMapping("/server-time")
    public Map<String, Object> getServerTime() {
        return Map.of("currentTime", OffsetDateTime.now());
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats(@RequestAttribute("tenantId") String tenantId) {
        return dashboardService.getAdminStats(getRequiredTenantId(tenantId));
    }

    // --- STAFF & SHIFTS ---
    @GetMapping("/staff")
    public Page<StaffMember> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return staffMemberService.getStaffPaged(getRequiredTenantId(tenantId), query, active, page, size);
    }

    @GetMapping("/staff/{id}")
    public StaffMember getStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return staffMemberService.getStaffMemberById(id)
                .filter(s -> s.getTenantId().equals(getRequiredTenantId(tenantId)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сотрудник не найден"));
    }

    @PutMapping("/staff/{id}/shift")
    public ResponseEntity<StaffShift> updateStaffShift(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody StaffShift shift) {
        shift.setStaffId(id);
        shift.setTenantId(getRequiredTenantId(tenantId));
        return ResponseEntity.ok(staffMemberService.saveShift(shift));
    }

    @PostMapping("/staff/{id}/shift/copy")
    public ResponseEntity<Void> copyStaffShift(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody StaffShift sourceShift, @RequestParam int days) {
        String tId = getRequiredTenantId(tenantId);
        log.info("📋 Copying shift for staff {} in branch {} for {} days", id, sourceShift.getBranchId(), days);
        
        for (int i = 1; i <= days; i++) {
            StaffShift newShift = new StaffShift();
            newShift.setStaffId(id);
            newShift.setTenantId(tId);
            newShift.setBranchId(sourceShift.getBranchId()); // ФИКС: Проставляем branchId
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

    @GetMapping("/clients/{id}")
    public Contact getContact(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return contactService.getContactById(id)
                .filter(c -> c.getTenantId().equals(getRequiredTenantId(tenantId)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клиент не найден"));
    }

    @PostMapping("/clients")
    public Contact createContact(@RequestAttribute("tenantId") String tenantId, @RequestBody Contact contact) {
        return contactService.addContact(contact, getRequiredTenantId(tenantId));
    }

    @PutMapping("/clients/{id}")
    public Contact updateContact(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody Contact contact) {
        return contactService.updateContact(id, contact, getRequiredTenantId(tenantId));
    }

    @DeleteMapping("/clients/{id}")
    public void deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
    }

    // --- RESOURCES ---
    @GetMapping("/resources")
    public List<Resource> getAllResources(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(value = "branch_id", required = false) String branchId) {
        return resourceService.getResources(getRequiredTenantId(tenantId), branchId);
    }

    @PostMapping("/resources")
    public Resource createResource(@RequestAttribute("tenantId") String tenantId, @RequestBody Resource resource) {
        return resourceService.addResource(resource, getRequiredTenantId(tenantId));
    }

    @PutMapping("/resources/{id}")
    public Resource updateResource(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody Resource details) {
        return resourceService.updateResource(id, details, getRequiredTenantId(tenantId));
    }

    @DeleteMapping("/resources/{id}")
    public void deleteResource(@PathVariable String id) {
        resourceService.deleteResource(id);
    }

    // --- APPOINTMENTS & SCHEDULE ---
    @GetMapping("/workload")
    public List<WorkloadDto> getWorkload(
            @RequestAttribute("tenantId") String tenantId, 
            @RequestParam("year") int year, 
            @RequestParam("month") int month,
            @RequestParam(value = "branch_id", required = false) String branchId) {
        return scheduleService.getWorkloadForMonth(getRequiredTenantId(tenantId), year, month, branchId);
    }

    @GetMapping("/appointments/day")
    public List<Appointment> getAppointmentsForDay(
            @RequestAttribute("tenantId") String tenantId, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "branch_id", required = false) String branchId) {
        return scheduleService.getAppointmentsForDay(date, getRequiredTenantId(tenantId), branchId);
    }

    @GetMapping("/schedule/staff")
    public List<StaffMember> getStaffForSchedule(
            @RequestAttribute("tenantId") String tenantId, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "branch_id", required = false) String branchId) {
        return staffMemberService.getStaffForDate(getRequiredTenantId(tenantId), date, branchId);
    }

    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Appointment appointment, @RequestAttribute("tenantId") String tenantId) {
        appointment.setTenantId(getRequiredTenantId(tenantId));
        return scheduleService.addAppointment(appointment);
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable String id, @RequestBody Appointment appointmentDetails) {
        return ResponseEntity.ok(scheduleService.updateAppointment(id, appointmentDetails));
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id) {
        scheduleService.deleteAppointment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/services")
    public List<com.tryneuro.backend.model.Service> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(getRequiredTenantId(tenantId));
    }
}
