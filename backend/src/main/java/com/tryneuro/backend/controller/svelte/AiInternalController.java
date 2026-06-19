package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.*;
import com.tryneuro.backend.dto.ai.*;
import com.tryneuro.backend.model.*;
import com.tryneuro.backend.service.*;
import com.tryneuro.backend.dto.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai/internal")
@RequiredArgsConstructor
public class AiInternalController {

    private final ContactService contactService;
    private final ScheduleService scheduleService;
    private final AppServiceService appServiceService;
    private final StaffMemberService staffMemberService;
    private final DashboardService dashboardService;
    private final ExportService exportService;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    private void validateSecret(String secret) {
        if (!internalSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid secret");
        }
    }

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant ID not found");
        }
        return tenantId;
    }

    @PostMapping("/contacts/search")
    public ResponseEntity<?> searchContact(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();

        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }

        String cleanQuery = query.replaceAll("[^0-9]", "");
        if (cleanQuery.length() >= 10) {
            Optional<Contact> byPhone = contactService.findContactByPhone(cleanQuery, tId);
            if (byPhone.isPresent()) {
                return ResponseEntity.ok(List.of(DtoMapper.toDto(byPhone.get())));
            }
        }

        var page = contactService.getContactsPaged(tId, query, true, 0, 10);
        return ResponseEntity.ok(page.getContent().stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/contacts")
    public ResponseEntity<?> createContact(
            @RequestBody AiContactCreateRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        Contact contact = new Contact();
        contact.setName(req.getName());
        contact.setPhones(List.of(req.getPhone()));
        contact.setEmail(req.getEmail());
        contact.setNotes(req.getNotes());

        Contact saved = contactService.addContact(contact, tId);
        return ResponseEntity.ok(DtoMapper.toDto(saved));
    }

    @PostMapping("/services/search")
    public ResponseEntity<?> searchService(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();

        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }

        List<Service> all = appServiceService.getAllServices(tId);
        List<Service> matches = all.stream()
                .filter(s -> s.getName().toLowerCase().contains(query.toLowerCase()))
                .limit(10)
                .toList();

        return ResponseEntity.ok(matches.stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/staff/search")
    public ResponseEntity<?> searchStaff(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();

        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }

        var page = staffMemberService.getStaffPaged(tId, query, true, 0, 10);
        return ResponseEntity.ok(page.getContent().stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/appointments")
    public ResponseEntity<?> createAppointment(
            @RequestBody AiCreateAppointmentRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getClientName() == null || req.getClientName().isBlank()) {
            return ResponseEntity.badRequest().body("Client name is required");
        }
        if (req.getDateTime() == null || req.getDateTime().isBlank()) {
            return ResponseEntity.badRequest().body("Date time is required");
        }

        String contactId = req.getContactId();
        String phone = req.getClientPhone();

        if (contactId == null && phone != null && !phone.isBlank()) {
            Optional<Contact> existing = contactService.findContactByPhone(phone, tId);
            if (existing.isPresent()) {
                contactId = existing.get().getId();
            } else {
                Contact newContact = new Contact();
                newContact.setName(req.getClientName());
                newContact.setPhones(List.of(phone));
                Contact saved = contactService.addContact(newContact, tId);
                contactId = saved.getId();
            }
        }

        Service service = null;
        if (req.getServiceName() != null && !req.getServiceName().isBlank()) {
            List<Service> all = appServiceService.getAllServices(tId);
            Optional<Service> found = all.stream()
                    .filter(s -> s.getName().toLowerCase().contains(req.getServiceName().toLowerCase()))
                    .findFirst();
            if (found.isPresent()) {
                service = found.get();
            } else {
                return ResponseEntity.badRequest().body("Service not found: " + req.getServiceName());
            }
        } else {
            return ResponseEntity.badRequest().body("Service name is required");
        }

        StaffMember staff = null;
        if (req.getStaffName() != null && !req.getStaffName().isBlank()) {
            var page = staffMemberService.getStaffPaged(tId, req.getStaffName(), true, 0, 5);
            if (!page.isEmpty()) {
                staff = page.getContent().get(0);
            }
        }

        OffsetDateTime startTime;
        try {
            startTime = OffsetDateTime.parse(req.getDateTime());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date time format: " + req.getDateTime());
        }

        Integer duration = req.getDurationMinutes();
        if (duration == null && service != null) {
            duration = service.getDurationInMinutes();
        }
        if (duration == null) {
            duration = 60;
        }

        Appointment appointment = new Appointment();
        appointment.setClientName(req.getClientName());
        appointment.setClientPhone(phone);
        appointment.setContactId(contactId);
        appointment.setService(service.getName());
        appointment.setStaffMemberId(staff != null ? staff.getId() : null);
        appointment.setBranchId(req.getBranchId());
        appointment.setStartTime(startTime);
        appointment.setDurationInMinutes(duration);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setTenantId(tId);

        try {
            AppointmentDto result = scheduleService.convertToDtoWithGroupStaff(
                    scheduleService.addAppointment(appointment, 
                            staff != null ? List.of(staff.getId()) : null, 
                            false));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create appointment", e);
            return ResponseEntity.badRequest().body("Failed to create appointment: " + e.getMessage());
        }
    }

    @PostMapping("/reports")
    public ResponseEntity<?> getReport(
            @RequestBody AiReportRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        LocalDate date;
        try {
            date = req.getDate() != null ? LocalDate.parse(req.getDate()) : LocalDate.now();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format: " + req.getDate());
        }

        String type = req.getReportType() != null ? req.getReportType() : "stats";
        String period = req.getPeriod() != null ? req.getPeriod() : "day";

        try {
            if ("stats".equalsIgnoreCase(type)) {
                Map<String, Object> stats = dashboardService.getAdminStats(tId);
                return ResponseEntity.ok(stats);
            } else if ("appointments".equalsIgnoreCase(type)) {
                LocalDate start, end;
                switch (period) {
                    case "week":
                        start = date.minusDays(7);
                        end = date;
                        break;
                    case "month":
                        start = date.withDayOfMonth(1);
                        end = date;
                        break;
                    default:
                        start = date;
                        end = date;
                }
                byte[] excel = exportService.exportAppointmentsToExcel(tId, null, start, end);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report_appointments_" + date + ".xlsx\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(Base64.getEncoder().encodeToString(excel));
            } else if ("clients".equalsIgnoreCase(type)) {
                byte[] excel = exportService.exportClientsToExcel(tId, null, true);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report_clients_" + date + ".xlsx\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(Base64.getEncoder().encodeToString(excel));
            } else {
                return ResponseEntity.badRequest().body("Unknown report type: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to generate report", e);
            return ResponseEntity.badRequest().body("Failed to generate report: " + e.getMessage());
        }
    }
}