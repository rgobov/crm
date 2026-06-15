package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.*;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ExportService exportService;

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
    public Page<StaffMemberDto> getStaffPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return staffMemberService.getStaffPaged(getRequiredTenantId(tenantId), query, active, page, size)
                .map(DtoMapper::toDto);
    }

    @GetMapping("/staff/{id}")
    public StaffMemberDto getStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return staffMemberService.getStaffMemberById(id)
                .filter(s -> s.getTenantId().equals(getRequiredTenantId(tenantId)))
                .map(DtoMapper::toDto)
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
            newShift.setBranchId(sourceShift.getBranchId());
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
    public StaffMemberDto createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return DtoMapper.toDto(staffMemberService.addStaffMember(request, getRequiredTenantId(tenantId)));
    }

    @PutMapping("/staff/{id}")
    public StaffMemberDto updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return DtoMapper.toDto(staffMemberService.updateStaffMember(id, request, getRequiredTenantId(tenantId)));
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }

    @PostMapping(value = "/staff/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StaffMemberDto uploadStaffPhoto(@RequestAttribute("tenantId") String tenantId, 
                                           @PathVariable String id, 
                                           @RequestParam("file") MultipartFile file) {
        return DtoMapper.toDto(staffMemberService.updateStaffPhoto(id, file, getRequiredTenantId(tenantId)));
    }

    @DeleteMapping("/staff/{id}/photo")
    public StaffMemberDto deleteStaffPhoto(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return DtoMapper.toDto(staffMemberService.deleteStaffPhoto(id, getRequiredTenantId(tenantId)));
    }

    // --- CLIENTS ---
    @GetMapping("/clients")
    public Page<ContactDto> getClientsPaged(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") boolean showAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return contactService.getContactsPaged(getRequiredTenantId(tenantId), query, showAll, page, size)
                .map(DtoMapper::toDto);
    }

    @GetMapping("/clients/{id}")
    public ContactDto getContact(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return contactService.getContactById(id)
                .filter(c -> c.getTenantId().equals(getRequiredTenantId(tenantId)))
                .map(DtoMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клиент не найден"));
    }

    @PostMapping("/clients")
    public ContactDto createContact(@RequestAttribute("tenantId") String tenantId, @RequestBody ContactDto contactDto) {
        Contact contact = DtoMapper.toEntity(contactDto, getRequiredTenantId(tenantId));
        return DtoMapper.toDto(contactService.addContact(contact, getRequiredTenantId(tenantId)));
    }

    @PutMapping("/clients/{id}")
    public ContactDto updateContact(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody ContactDto contactDto) {
        Contact contact = DtoMapper.toEntity(contactDto, getRequiredTenantId(tenantId));
        return DtoMapper.toDto(contactService.updateContact(id, contact, getRequiredTenantId(tenantId)));
    }

    @DeleteMapping("/clients/{id}")
    public void deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
    }

    // --- RESOURCES ---
    @GetMapping("/resources")
    public List<ResourceDto> getAllResources(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId) {
        return resourceService.getResources(getRequiredTenantId(tenantId), branchId)
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @PostMapping("/resources")
    public ResourceDto createResource(@RequestAttribute("tenantId") String tenantId, @RequestBody ResourceDto resourceDto) {
        Resource resource = DtoMapper.toEntity(resourceDto, getRequiredTenantId(tenantId));
        return DtoMapper.toDto(resourceService.addResource(resource, getRequiredTenantId(tenantId)));
    }

    @PutMapping("/resources/{id}")
    public ResourceDto updateResource(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody ResourceDto details) {
        Resource resource = DtoMapper.toEntity(details, getRequiredTenantId(tenantId));
        return DtoMapper.toDto(resourceService.updateResource(id, resource, getRequiredTenantId(tenantId)));
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
            @RequestParam(value = "branchId", required = false) String branchId) {
        return scheduleService.getWorkloadForMonth(getRequiredTenantId(tenantId), year, month, branchId);
    }

    @GetMapping("/appointments/day")
    public List<AppointmentDto> getAppointmentsForDay(
            @RequestAttribute("tenantId") String tenantId, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "branchId", required = false) String branchId) {
        return scheduleService.getAppointmentsForDay(date, getRequiredTenantId(tenantId), branchId)
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/schedule/staff")
    public List<StaffScheduleDto> getStaffForSchedule(
            @RequestAttribute("tenantId") String tenantId, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "branchId", required = false) String branchId) {
        return staffMemberService.getStaffForDate(getRequiredTenantId(tenantId), date, branchId)
                .stream().map(DtoMapper::toScheduleDto).collect(Collectors.toList());
    }
    
    @GetMapping("/schedule/staff/{id}/photo")
    public ResponseEntity<Map<String, String>> getStaffPhoto(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return staffMemberService.getStaffMemberById(id)
                .filter(s -> s.getTenantId().equals(getRequiredTenantId(tenantId)))
                .map(staff -> {
                    String photoDataBase64 = null;
                    if (staff.getPhotoData() != null && staff.getPhotoData().length > 0) {
                        photoDataBase64 = Base64.getEncoder().encodeToString(staff.getPhotoData());
                    }
                    // Fix: Map.of() doesn't accept null values
                    return ResponseEntity.ok(Map.of("photoData", photoDataBase64 != null ? photoDataBase64 : ""));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/appointments")
    public AppointmentDto createAppointment(@RequestBody AppointmentDto appointmentDto, @RequestAttribute("tenantId") String tenantId) {
        Appointment appointment = DtoMapper.toEntity(appointmentDto, getRequiredTenantId(tenantId));
        return DtoMapper.toDto(scheduleService.addAppointment(appointment));
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<AppointmentDto> updateAppointment(@PathVariable String id, @RequestBody AppointmentDto appointmentDetails, @RequestAttribute("tenantId") String tenantId) {
        Appointment appointment = DtoMapper.toEntity(appointmentDetails, getRequiredTenantId(tenantId));
        return ResponseEntity.ok(DtoMapper.toDto(scheduleService.updateAppointment(id, appointment)));
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id) {
        scheduleService.deleteAppointment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/services")
    public List<ServiceDto> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(getRequiredTenantId(tenantId))
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/clients/export")
    public ResponseEntity<byte[]> exportClients(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") boolean showAll) {
        try {
            byte[] excelBytes = exportService.exportClientsToExcel(getRequiredTenantId(tenantId), query, showAll);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clients.xlsx\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Error exporting clients", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка экспорта клиентов");
        }
    }

    @GetMapping("/appointments/export")
    public ResponseEntity<byte[]> exportAppointments(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String contactId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] excelBytes = exportService.exportAppointmentsToExcel(getRequiredTenantId(tenantId), contactId, startDate, endDate);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visits.xlsx\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Error exporting appointments", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка экспорта визитов");
        }
    }
}
