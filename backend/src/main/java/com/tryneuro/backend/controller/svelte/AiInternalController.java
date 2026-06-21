package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.*;
import com.tryneuro.backend.dto.ai.*;
import com.tryneuro.backend.model.*;
import com.tryneuro.backend.service.*;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.dto.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ContactRepository contactRepository;
    private final ScheduleService scheduleService;
    private final AppServiceService appServiceService;
    private final StaffMemberService staffMemberService;
    private final StaffMemberRepository staffMemberRepository;
    private final DashboardService dashboardService;
    private final ExportService exportService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserAiConfigService userAiConfigService;

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

    private void checkRole(String actorRole, String... allowed) {
        if (actorRole == null || actorRole.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "X-Actor-Role header is required");
        }
        for (String a : allowed) {
            if (a.equalsIgnoreCase(actorRole)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role " + actorRole + " not allowed for this action");
    }

    private String getRequiredActorContactId(String actorContactId) {
        if (actorContactId == null || actorContactId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "contactId required for CLIENT role");
        }
        return actorContactId;
    }

    @PostMapping("/contacts/search")
    public ResponseEntity<?> searchContact(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE");
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
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");
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
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
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

        // CLIENT can only book for themselves
        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            contactId = getRequiredActorContactId(actorContactId);
            Optional<Contact> opt = contactService.getContactById(contactId);
            if (opt.isPresent()) {
                Contact c = opt.get();
                if (phone == null && c.getPhones() != null && !c.getPhones().isEmpty()) {
                    phone = c.getPhones().get(0);
                }
            }
        }

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

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable String id,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
        validateSecret(secret);
        String tId = getRequiredTenantId(tenantId);

        Optional<Appointment> opt = scheduleService.getAppointmentById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Appointment app = opt.get();

        if (!tId.equals(app.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Appointment does not belong to this tenant");
        }

        // CLIENT can only cancel their own appointments
        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            String cId = getRequiredActorContactId(actorContactId);
            if (!cId.equals(app.getContactId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot cancel another client's appointment");
            }
        }

        scheduleService.deleteAppointment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments/my")
    public ResponseEntity<?> getMyAppointments(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        validateSecret(secret);
        String tId = getRequiredTenantId(tenantId);

        List<Appointment> apps;
        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            String contactId = getRequiredActorContactId(actorContactId);
            apps = scheduleService.getAppointmentsForContact(contactId, tId);
        } else if ("EMPLOYEE".equalsIgnoreCase(actorRole)) {
            if (actorStaffId == null || actorStaffId.isEmpty()) {
                return ResponseEntity.badRequest().body("staffId required for EMPLOYEE role");
            }
            apps = scheduleService.getAppointmentsForStaffAll(tId, actorStaffId);
        } else {
            apps = scheduleService.getAppointmentsByTenant(tId);
        }
        return ResponseEntity.ok(apps.stream().map(scheduleService::convertToDtoWithGroupStaff).toList());
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<?> getNotificationPreferences(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        validateSecret(secret);
        String tId = getRequiredTenantId(tenantId);

        String contactId;
        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            contactId = getRequiredActorContactId(actorContactId);
        } else {
            return ResponseEntity.badRequest().body("Only CLIENT role can manage notification preferences via this endpoint");
        }

        try {
            return ResponseEntity.ok(contactService.getNotificationPreferences(contactId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Contact not found: " + e.getMessage());
        }
    }

    @PutMapping("/notifications/preferences")
    public ResponseEntity<?> updateNotificationPreferences(
            @RequestBody AiNotificationPreferencesDto req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        validateSecret(secret);
        String tId = getRequiredTenantId(tenantId);

        String contactId;
        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            contactId = getRequiredActorContactId(actorContactId);
        } else {
            return ResponseEntity.badRequest().body("Only CLIENT role can manage notification preferences via this endpoint");
        }

        try {
            contactService.updateNotificationPreferences(contactId, req.isNotificationEnabled(), req.getNotificationLeadTimeHours());
            return ResponseEntity.ok(contactService.getNotificationPreferences(contactId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update: " + e.getMessage());
        }
    }

    @GetMapping("/users/by-telegram/{telegramId}")
    public ResponseEntity<?> getUserByTelegramId(
            @PathVariable Long telegramId,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User u = userOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("role", u.getRole() != null ? u.getRole().name() : "CLIENT");
        result.put("contactId", u.getContactId());
        result.put("staffId", u.getStaffId());
        result.put("tenantId", u.getTenantId());
        result.put("email", u.getEmail());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tenant/by-telegram/{chatId}")
    public ResponseEntity<?> getUserIdByTelegramId(
            @PathVariable Long chatId,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        
        // 1. Check User.telegram_id
        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "userId", u.getId(),
                    "tenantId", u.getTenantId(),
                    "source", "user"
            ));
        }
        
        // 2. Check Contact.telegram_id
        Optional<Contact> contactOpt = contactService.findByTelegramId(chatId);
        if (contactOpt.isPresent()) {
            Contact c = contactOpt.get();
            // Get User from Contact
            Optional<User> contactUserOpt = userRepository.findByContactId(c.getId());
            if (contactUserOpt.isPresent()) {
                User u = contactUserOpt.get();
                return ResponseEntity.ok(Map.of(
                        "userId", u.getId(),
                        "tenantId", u.getTenantId(),
                        "source", "contact"
                ));
            }
        }
        
        // 3. Check StaffMember.telegram_id
        Optional<StaffMember> staffOpt = staffMemberService.findByTelegramId(chatId);
        if (staffOpt.isPresent()) {
            StaffMember s = staffOpt.get();
            // Get User from StaffMember
            if (s.getUserId() != null) {
                Optional<User> staffUserOpt = userRepository.findById(s.getUserId());
                if (staffUserOpt.isPresent()) {
                    User u = staffUserOpt.get();
                    return ResponseEntity.ok(Map.of(
                            "userId", u.getId(),
                            "tenantId", u.getTenantId(),
                            "source", "staff"
                    ));
                }
            }
        }
        
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user-config/{userId}")
    public ResponseEntity<?> getUserConfig(
            @PathVariable String userId,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        UserAiConfig config = userAiConfigService.getConfig(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "llm_provider", config.getLlmProvider(),
                "llm_model", config.getLlmModel(),
                "api_key", config.getApiKey(),
                "stt_provider", config.getSttProvider()
        ));
    }

    @PostMapping("/telegram/bind")
    public ResponseEntity<?> bindTelegramId(
            @RequestBody Map<String, Object> req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        
        String type = (String) req.get("type"); // "contact" | "staff" | "user"
        String id = (String) req.get("id");     // entity id
        Long telegramId = ((Number) req.get("telegram_id")).longValue();
        
        switch (type) {
            case "contact" -> {
                Optional<Contact> contactOpt = contactService.getContactById(id);
                if (contactOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Contact not found");
                }
                Contact c = contactOpt.get();
                c.setTelegramId(telegramId);
                contactRepository.save(c);
                return ResponseEntity.ok(Map.of("status", "bound", "type", "contact"));
            }
            case "staff" -> {
                Optional<StaffMember> staffOpt = staffMemberService.getStaffMemberById(id);
                if (staffOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Staff member not found");
                }
                StaffMember s = staffOpt.get();
                s.setTelegramId(telegramId);
                staffMemberRepository.save(s);
                return ResponseEntity.ok(Map.of("status", "bound", "type", "staff"));
            }
            case "user" -> {
                Optional<User> userOpt = userRepository.findById(id);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("User not found");
                }
                User u = userOpt.get();
                u.setTelegramId(telegramId);
                userRepository.save(u);
                return ResponseEntity.ok(Map.of("status", "bound", "type", "user"));
            }
            default -> {
                return ResponseEntity.badRequest().body("Invalid type: " + type);
            }
        }
    }

    @PostMapping("/reports")
    public ResponseEntity<?> getReport(
            @RequestBody AiReportRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");
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
