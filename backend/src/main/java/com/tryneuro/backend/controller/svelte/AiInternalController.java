package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.*;
import com.tryneuro.backend.dto.ai.*;
import com.tryneuro.backend.model.*;
import com.tryneuro.backend.service.*;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.repository.BranchRepository;
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
    private final BranchService branchService;
    private final DashboardService dashboardService;
    private final ExportService exportService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserAiConfigService userAiConfigService;
    private final AiKnowledgeService aiKnowledgeService;
    private final ResourceService resourceService;
    private final RagSearchService ragSearchService;
    private final KnowledgeIngestService knowledgeIngestService;
    private final BranchRepository branchRepository;
    private final DateResolver dateResolver;

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

    private void assertActorBelongsToTenant(String actorRole, String actorStaffId,
                                            String actorContactId, String tId) {
        if ("EMPLOYEE".equalsIgnoreCase(actorRole) && actorStaffId != null && !actorStaffId.isBlank()) {
            Optional<StaffMember> s = staffMemberService.getStaffMemberById(actorStaffId);
            if (s.isEmpty() || !tId.equals(s.get().getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff does not belong to this tenant");
            }
        }
        if ("CLIENT".equalsIgnoreCase(actorRole) && actorContactId != null && !actorContactId.isBlank()) {
            Optional<Contact> c = contactService.getContactById(actorContactId);
            if (c.isEmpty() || !tId.equals(c.get().getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contact does not belong to this tenant");
            }
        }
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

        if (query == null || query.isBlank()) {
            var page = contactService.getContactsPaged(tId, "", true, 0, 50);
            return ResponseEntity.ok(page.getContent().stream().map(DtoMapper::toDto).toList());
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

        List<Service> all = appServiceService.getAllServices(tId);
        List<Service> matches;
        if (query == null || query.isBlank()) {
            matches = all.stream().limit(50).toList();
        } else {
            matches = all.stream()
                    .filter(s -> s.getName().toLowerCase().contains(query.toLowerCase()))
                    .limit(50)
                    .toList();
        }

        return ResponseEntity.ok(matches.stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/resources/search")
    public ResponseEntity<?> searchResources(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        List<Resource> all = resourceService.getResources(tId, null);
        String query = req.getQuery();
        if (query != null && !query.isBlank()) {
            all = all.stream()
                    .filter(r -> r.getName().toLowerCase().contains(query.toLowerCase()))
                    .limit(10)
                    .toList();
        }
        return ResponseEntity.ok(all.stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/staff/search")
    public ResponseEntity<?> searchStaff(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();
        String branchId = req.getBranchId();

        List<StaffMember> staff;
        if (branchId != null && !branchId.isBlank()) {
            staff = staffMemberRepository.findByTenantIdAndBranchIdWithBranches(tId, branchId);
        } else {
            var page = staffMemberService.getStaffPaged(tId, query != null ? query : "", true, 0, 50);
            staff = page.getContent();
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            staff = staff.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                            || (s.getSpecialty() != null && s.getSpecialty().toLowerCase().contains(q))
                            || (s.getPhone() != null && s.getPhone().contains(q)))
                    .toList();
        }

        return ResponseEntity.ok(staff.stream().map(DtoMapper::toDto).toList());
    }

    @PostMapping("/appointments")
    public ResponseEntity<?> createAppointment(
            @RequestBody AiCreateAppointmentRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE", "CLIENT");
        String tId = getRequiredTenantId(req.getTenantId());
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);

        if (req.getClientName() == null || req.getClientName().isBlank()) {
            return ResponseEntity.badRequest().body("Client name is required");
        }
        boolean hasDateTime = req.getDateTime() != null && !req.getDateTime().isBlank();
        boolean hasAlt = req.getBranchId() != null && !req.getBranchId().isBlank()
                && req.getDate() != null && !req.getDate().isBlank()
                && req.getTime() != null && !req.getTime().isBlank();
        if (!hasDateTime && !hasAlt) {
            return ResponseEntity.badRequest().body("dateTime (ISO) OR (branchId + date + time) is required");
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
        if (req.getStaffId() != null && !req.getStaffId().isBlank()) {
            Optional<StaffMember> byId = staffMemberService.getStaffMemberById(req.getStaffId());
            if (byId.isPresent() && tId.equals(byId.get().getTenantId())) {
                staff = byId.get();
            }
        }
        if (staff == null && req.getStaffName() != null && !req.getStaffName().isBlank()) {
            var page = staffMemberService.getStaffPaged(tId, req.getStaffName(), true, 0, 5);
            if (!page.isEmpty()) {
                staff = page.getContent().get(0);
            }
        }

        OffsetDateTime startTime;
        try {
            if (req.getDateTime() != null && !req.getDateTime().isBlank()) {
                startTime = OffsetDateTime.parse(req.getDateTime());
            } else if (req.getBranchId() != null && !req.getBranchId().isBlank()
                    && req.getDate() != null && !req.getDate().isBlank()
                    && req.getTime() != null && !req.getTime().isBlank()) {
                Optional<Branch> branchOpt = branchRepository.findById(req.getBranchId());
                if (branchOpt.isEmpty() || !tId.equals(branchOpt.get().getTenantId())) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Branch not found: " + req.getBranchId());
                }
                Branch branch = branchOpt.get();
                LocalDate localDate = dateResolver.resolve(branch, req.getDate());
                LocalTime localTime = LocalTime.parse(req.getTime());
                java.time.ZoneId zoneId;
                try {
                    zoneId = java.time.ZoneId.of(branch.getTimezone());
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Invalid branch timezone: " + branch.getTimezone());
                }
                startTime = java.time.ZonedDateTime.of(localDate, localTime, zoneId).toOffsetDateTime();
            } else {
                return ResponseEntity.badRequest().body("dateTime (ISO) OR (branchId + date + time) is required");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date/time format: " + e.getMessage());
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
        appointment.setResourceId(req.getResourceId());
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
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE", "CLIENT");
        String tId = getRequiredTenantId(tenantId);
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);

        Optional<Appointment> opt = scheduleService.getAppointmentById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Appointment app = opt.get();

        if (!tId.equals(app.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Appointment does not belong to this tenant");
        }

        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            String cId = getRequiredActorContactId(actorContactId);
            if (!cId.equals(app.getContactId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot cancel another client's appointment");
            }
        }

        if ("EMPLOYEE".equalsIgnoreCase(actorRole)) {
            if (actorStaffId == null || actorStaffId.isEmpty()) {
                return ResponseEntity.badRequest().body("staffId required for EMPLOYEE role");
            }
            if (!actorStaffId.equals(app.getStaffMemberId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot cancel another employee's appointment");
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
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);

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

    @GetMapping("/appointments/{id}")
    public ResponseEntity<?> getAppointment(
            @PathVariable String id,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE", "CLIENT");
        String tId = getRequiredTenantId(tenantId);
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);

        Optional<Appointment> opt = scheduleService.getAppointmentById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Appointment app = opt.get();

        if (!tId.equals(app.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Appointment does not belong to this tenant");
        }

        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            String cId = getRequiredActorContactId(actorContactId);
            if (!cId.equals(app.getContactId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot view another client's appointment");
            }
        }

        if ("EMPLOYEE".equalsIgnoreCase(actorRole)) {
            if (actorStaffId == null || actorStaffId.isEmpty()) {
                return ResponseEntity.badRequest().body("staffId required for EMPLOYEE role");
            }
            if (!actorStaffId.equals(app.getStaffMemberId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot view another employee's appointment");
            }
        }

        return ResponseEntity.ok(scheduleService.convertToDtoWithGroupStaff(app));
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String id,
            @RequestBody AiCreateAppointmentRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Actor-Role") String actorRole,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE", "CLIENT");
        String tId = getRequiredTenantId(req.getTenantId());
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);

        Optional<Appointment> existingOpt = scheduleService.getAppointmentById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Appointment existing = existingOpt.get();

        if (!tId.equals(existing.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Appointment does not belong to this tenant");
        }

        if ("CLIENT".equalsIgnoreCase(actorRole)) {
            String cId = getRequiredActorContactId(actorContactId);
            if (!cId.equals(existing.getContactId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot update another client's appointment");
            }
        }

        if ("EMPLOYEE".equalsIgnoreCase(actorRole)) {
            if (actorStaffId == null || actorStaffId.isEmpty()) {
                return ResponseEntity.badRequest().body("staffId required for EMPLOYEE role");
            }
            if (!actorStaffId.equals(existing.getStaffMemberId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot update another employee's appointment");
            }
        }

        if (req.getClientName() != null && !req.getClientName().isBlank()) {
            existing.setClientName(req.getClientName());
        }
        if (req.getClientPhone() != null && !req.getClientPhone().isBlank()) {
            existing.setClientPhone(req.getClientPhone());
        }
        if (req.getServiceName() != null && !req.getServiceName().isBlank()) {
            List<Service> all = appServiceService.getAllServices(tId);
            Optional<Service> found = all.stream()
                    .filter(s -> s.getName().toLowerCase().contains(req.getServiceName().toLowerCase()))
                    .findFirst();
            if (found.isPresent()) {
                existing.setService(found.get().getName());
            }
        }
        if (req.getStaffName() != null && !req.getStaffName().isBlank()) {
            var page = staffMemberService.getStaffPaged(tId, req.getStaffName(), true, 0, 5);
            if (!page.isEmpty()) {
                StaffMember staff = page.getContent().get(0);
                existing.setStaffMemberId(staff.getId());
            }
        }
        if (req.getBranchId() != null && !req.getBranchId().isBlank()) {
            existing.setBranchId(req.getBranchId());
        }
        if (req.getResourceId() != null && !req.getResourceId().isBlank()) {
            existing.setResourceId(req.getResourceId());
        }
        if (req.getDateTime() != null && !req.getDateTime().isBlank()) {
            try {
                existing.setStartTime(OffsetDateTime.parse(req.getDateTime()));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid date time format: " + req.getDateTime());
            }
        }
        if (req.getDurationMinutes() != null) {
            existing.setDurationInMinutes(req.getDurationMinutes());
        }

        try {
            AppointmentDto result = scheduleService.convertToDtoWithGroupStaff(
                    scheduleService.updateAppointment(id, existing));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to update appointment", e);
            return ResponseEntity.badRequest().body("Failed to update appointment: " + e.getMessage());
        }
    }

    @GetMapping("/branches")
    public ResponseEntity<?> getBranches(
            @RequestParam(value = "query", required = false) String query,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        validateSecret(secret);
        String tId = getRequiredTenantId(tenantId);

        List<Branch> branches = branchService.getBranches(tId);
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            branches = branches.stream()
                    .filter(b -> (b.getName() != null && b.getName().toLowerCase().contains(q))
                            || (b.getAddress() != null && b.getAddress().toLowerCase().contains(q)))
                    .toList();
        }
        List<BranchDto> dtos = branches.stream().map(DtoMapper::toDto).toList();
        List<String> timezones = dtos.stream().map(BranchDto::getTimezone).distinct().toList();
        boolean ambiguous = timezones.size() > 1;
        return ResponseEntity.ok(Map.of(
                "branches", dtos,
                "ambiguous", ambiguous,
                "timezones", timezones
        ));
    }

    @PostMapping("/branches/resolve")
    public ResponseEntity<?> resolveBranch(
            @RequestBody AiBranchResolveRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("query is required");
        }

        List<Branch> all = branchService.getBranches(tId);
        String q = query.toLowerCase();
        List<Branch> matches = all.stream()
                .filter(b -> (b.getName() != null && b.getName().toLowerCase().contains(q))
                        || (b.getAddress() != null && b.getAddress().toLowerCase().contains(q)))
                .toList();

        if (matches.isEmpty()) {
            return ResponseEntity.ok(Map.of("matched", false));
        }

        List<Map<String, String>> branchList = matches.stream().map(b -> Map.of(
                "branchId", b.getId(),
                "branchName", b.getName() != null ? b.getName() : "",
                "timezone", b.getTimezone() != null ? b.getTimezone() : ""
        )).toList();

        List<String> tzs = matches.stream().map(Branch::getTimezone).distinct().toList();
        boolean ambiguous = tzs.size() > 1;

        if (ambiguous) {
            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "ambiguous", true,
                    "branches", branchList
            ));
        }

        Branch matched = matches.get(0);
        return ResponseEntity.ok(Map.of(
                "matched", true,
                "ambiguous", false,
                "branchId", matched.getId(),
                "branchName", matched.getName() != null ? matched.getName() : "",
                "timezone", matched.getTimezone() != null ? matched.getTimezone() : ""
        ));
    }

    @PostMapping("/availability/slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestBody AiSlotsRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getStaffId() == null || req.getStaffId().isBlank()) {
            return ResponseEntity.badRequest().body("staffId is required");
        }
        if (req.getDate() == null || req.getDate().isBlank()) {
            return ResponseEntity.badRequest().body("date is required");
        }
        int duration = (req.getDuration() != null && req.getDuration() > 0) ? req.getDuration() : 60;
        String branchId = req.getBranchId();

        LocalDate date;
        try {
            if (isRelativeDate(req.getDate())) {
                if (branchId == null || branchId.isBlank()) {
                    return ResponseEntity.badRequest().body("branchId is required for relative date '" + req.getDate() + "'");
                }
                Optional<Branch> branchOpt = branchRepository.findById(branchId);
                if (branchOpt.isEmpty() || !tId.equals(branchOpt.get().getTenantId())) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Branch not found: " + branchId);
                }
                date = dateResolver.resolve(branchOpt.get(), req.getDate());
            } else {
                date = LocalDate.parse(req.getDate());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date: " + e.getMessage());
        }

        List<Map<String, String>> slots = scheduleService.getAvailableSlotsForBranch(tId, req.getStaffId(), branchId, date, duration);
        return ResponseEntity.ok(Map.of("slots", slots));
    }

    @PostMapping("/availability/branch-slots")
    public ResponseEntity<?> getBranchStaffSlots(
            @RequestBody AiBranchSlotsRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getBranchId() == null || req.getBranchId().isBlank()) {
            return ResponseEntity.badRequest().body("branchId is required");
        }
        if (req.getDate() == null || req.getDate().isBlank()) {
            return ResponseEntity.badRequest().body("date is required");
        }
        int duration = (req.getDuration() != null && req.getDuration() > 0) ? req.getDuration() : 60;

        Optional<Branch> branchOpt = branchRepository.findById(req.getBranchId());
        if (branchOpt.isEmpty() || !tId.equals(branchOpt.get().getTenantId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Branch not found: " + req.getBranchId());
        }
        Branch branch = branchOpt.get();

        LocalDate date;
        try {
            date = dateResolver.resolve(branch, req.getDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date: " + e.getMessage());
        }

        List<StaffMember> staff = staffMemberRepository.findByTenantIdAndBranchIdWithBranches(tId, req.getBranchId());
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (StaffMember s : staff) {
            if (!s.isActive()) continue;
            List<Map<String, String>> slots = scheduleService.getAvailableSlotsForBranch(tId, s.getId(), branch.getId(), date, duration);
            result.add(Map.of(
                    "staffId", s.getId(),
                    "staffName", s.getName() != null ? s.getName() : "",
                    "slots", slots
            ));
        }
        return ResponseEntity.ok(Map.of(
                "branchId", branch.getId(),
                "branchName", branch.getName() != null ? branch.getName() : "",
                "timezone", branch.getTimezone(),
                "date", date.toString(),
                "staff", result
        ));
    }

    private boolean isRelativeDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return false;
        return !dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    @PostMapping("/availability")
    public ResponseEntity<?> checkAvailability(
            @RequestBody AiAvailabilityRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getStaffId() == null || req.getStaffId().isBlank()) {
            return ResponseEntity.badRequest().body("staffId is required");
        }
        if (req.getDate() == null || req.getDate().isBlank()) {
            return ResponseEntity.badRequest().body("date is required");
        }
        if (req.getTime() == null || req.getTime().isBlank()) {
            return ResponseEntity.badRequest().body("time is required");
        }
        if (req.getDuration() == null || req.getDuration() <= 0) {
            return ResponseEntity.badRequest().body("duration is required");
        }

        try {
            LocalDate date = LocalDate.parse(req.getDate());
            LocalTime time = LocalTime.parse(req.getTime());
            boolean staffAvailable = scheduleService.isStaffMemberAvailable(
                    tId, req.getStaffId(), date, time, req.getDuration(), null, null);
            if (!staffAvailable) {
                return ResponseEntity.ok(Map.of("available", false, "reason", "staff_busy"));
            }
            if (req.getResourceId() != null && !req.getResourceId().isBlank()) {
                boolean resourceAvailable = scheduleService.isResourceAvailable(
                        tId, req.getResourceId(), date, time, req.getDuration(), null);
                if (!resourceAvailable) {
                    return ResponseEntity.ok(Map.of("available", false, "reason", "resource_busy"));
                }
            }
            return ResponseEntity.ok(Map.of("available", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date/time format: " + e.getMessage());
        }
    }

    @PostMapping("/staff/schedule")
    public ResponseEntity<?> getStaffSchedule(
            @RequestBody AiStaffScheduleRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getStaffId() == null || req.getStaffId().isBlank()) {
            return ResponseEntity.badRequest().body("staffId is required");
        }
        if (req.getDate() == null || req.getDate().isBlank()) {
            return ResponseEntity.badRequest().body("date is required");
        }

        try {
            LocalDate date = LocalDate.parse(req.getDate());
            Optional<StaffMember> staffOpt = staffMemberService.getStaffByIdAndDate(req.getStaffId(), date, null);
            if (staffOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!tId.equals(staffOpt.get().getTenantId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Staff does not belong to this tenant");
            }
            return ResponseEntity.ok(DtoMapper.toScheduleDto(staffOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format: " + e.getMessage());
        }
    }

    @GetMapping("/contacts/{id}")
    public ResponseEntity<?> getContact(
            @PathVariable String id,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER", "EMPLOYEE");

        Optional<Contact> opt = contactService.getContactById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String tId = getRequiredTenantId(opt.get().getTenantId());
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);
        if (!tId.equals(opt.get().getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Contact does not belong to this tenant");
        }
        return ResponseEntity.ok(DtoMapper.toDto(opt.get()));
    }

    @PutMapping("/contacts/{id}")
    public ResponseEntity<?> updateContact(
            @PathVariable String id,
            @RequestBody ContactDto req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");
        String tId = getRequiredTenantId(tenantId);

        Optional<Contact> existing = contactService.getContactById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);
        if (!tId.equals(existing.get().getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Contact does not belong to this tenant");
        }

        try {
            Contact updated = contactService.updateContact(id, DtoMapper.toEntity(req, tId), tId);
            return ResponseEntity.ok(DtoMapper.toDto(updated));
        } catch (Exception e) {
            log.error("Failed to update contact", e);
            return ResponseEntity.badRequest().body("Failed to update contact: " + e.getMessage());
        }
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<?> deleteContact(
            @PathVariable String id,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");

        Optional<Contact> existing = contactService.getContactById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String tId = getRequiredTenantId(existing.get().getTenantId());
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);
        if (!tId.equals(existing.get().getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Contact does not belong to this tenant");
        }

        try {
            contactService.deleteContact(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete contact", e);
            return ResponseEntity.badRequest().body("Failed to delete contact: " + e.getMessage());
        }
    }

    @PostMapping("/services")
    public ResponseEntity<?> createService(
            @RequestBody AiServiceCreateRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Service name is required");
        }

        Service service = new Service();
        service.setName(req.getName());
        service.setDurationInMinutes(req.getDurationMinutes());
        service.setPriceMin(req.getPriceMin());
        service.setPriceMax(req.getPriceMax());

        try {
            Service saved = appServiceService.addService(service, tId);
            return ResponseEntity.ok(DtoMapper.toDto(saved));
        } catch (Exception e) {
            log.error("Failed to create service", e);
            return ResponseEntity.badRequest().body("Failed to create service: " + e.getMessage());
        }
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<?> updateService(
            @PathVariable String id,
            @RequestBody AiServiceCreateRequest req,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");
        String tId = getRequiredTenantId(req.getTenantId());

        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Service name is required");
        }

        Service service = new Service();
        service.setName(req.getName());
        service.setDurationInMinutes(req.getDurationMinutes());
        service.setPriceMin(req.getPriceMin());
        service.setPriceMax(req.getPriceMax());

        try {
            Service updated = appServiceService.updateService(id, service, tId);
            return ResponseEntity.ok(DtoMapper.toDto(updated));
        } catch (Exception e) {
            log.error("Failed to update service", e);
            return ResponseEntity.badRequest().body("Failed to update service: " + e.getMessage());
        }
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<?> deleteService(
            @PathVariable String id,
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMIN") String actorRole,
            @RequestHeader(value = "X-Actor-Staff-Id", required = false) String actorStaffId,
            @RequestHeader(value = "X-Actor-Contact-Id", required = false) String actorContactId) {
        validateSecret(secret);
        checkRole(actorRole, "ADMIN", "MANAGER");

        Optional<Service> existing = appServiceService.getServiceById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String tId = getRequiredTenantId(existing.get().getTenantId());
        assertActorBelongsToTenant(actorRole, actorStaffId, actorContactId, tId);
        if (!tId.equals(existing.get().getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Service does not belong to this tenant");
        }

        try {
            appServiceService.deleteService(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete service", e);
            return ResponseEntity.badRequest().body("Failed to delete service: " + e.getMessage());
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

    @GetMapping("/instructions")
    public ResponseEntity<?> getInstructions(
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        Map<String, List<String>> instructions = new LinkedHashMap<>();
        instructions.put("create_appointment", List.of(
                "Поиск клиента через search_contacts. Если не найден — спроси телефон и создай через create_contact.",
                "Поиск услуги через search_services. Если не найдена — покажи все услуги или предложи создать (ADMIN/MANAGER).",
                "Поиск мастера через search_staff. Если не указан — покажи всех доступных.",
                "Поиск ресурса через search_resources (кабинет, оборудование). Если услуга требует ресурс — укажи его.",
                "Проверка слотов через check_availability с staffId и resourceId.",
                "Создание записи через create_appointment с resourceId если нужен.",
                "Подтверди пользователю: дата, время, мастер, услуга, ресурс."
        ));
        instructions.put("search_contacts", List.of(
                "search_contacts по имени или телефону.",
                "Если найден — покажи данные.",
                "Если НЕ найден — спроси телефон для создания через create_contact."
        ));
        instructions.put("search_services", List.of(
                "search_services по названию.",
                "Если не найдена — покажи все услуги (search_services с пустым query).",
                "Если все пусто — предложи создать (ADMIN/MANAGER).",
                "CLIENT — сообщи что услуг нет."
        ));
        instructions.put("search_staff", List.of(
                "search_staff по имени.",
                "Если не найден — покажи всех (search_staff с пустым query).",
                "Если пусто — сообщи что сотрудников нет."
        ));
        instructions.put("search_resources", List.of(
                "search_resources по названию кабинета/оборудования.",
                "Если не найден — покажи все (search_resources с пустым query)."
        ));
        return ResponseEntity.ok(instructions);
    }

    @PostMapping("/knowledge/search")
    public ResponseEntity<?> searchKnowledge(
            @RequestBody AiSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        String query = req.getQuery();

        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }

        var results = aiKnowledgeService.search(tId, query);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/knowledge/rag-search")
    public ResponseEntity<?> ragSearch(
            @RequestBody AiRagSearchRequest req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.getTenantId());
        if (req.getQuery() == null || req.getQuery().isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }
        int topK = req.getTopK() > 0 ? req.getTopK() : 5;
        var result = ragSearchService.search(tId, req.getQuery(), topK);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/knowledge/ingest")
    public ResponseEntity<?> ingestKnowledge(
            @RequestBody Map<String, String> req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.get("tenantId"));
        String knowledgeId = req.get("knowledgeId");
        String text = req.get("text");
        if (knowledgeId == null || text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().body("knowledgeId and text are required");
        }
        knowledgeIngestService.ingest(tId, knowledgeId, text);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/knowledge/reindex")
    public ResponseEntity<?> reindexKnowledge(
            @RequestBody Map<String, String> req,
            @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String tId = getRequiredTenantId(req.get("tenantId"));
        knowledgeIngestService.reindex(tId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
