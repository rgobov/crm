package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.*;
import com.tryneuro.backend.model.*;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final BranchService branchService;
    private final AppServiceService appServiceService;
    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService;
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final ResourceService resourceService;

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        return tenantId;
    }

    @GetMapping("/branches")
    public List<BranchDto> getBranches(@RequestAttribute("tenantId") String tenantId) {
        return branchService.getBranches(getRequiredTenantId(tenantId))
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/services")
    public List<ServiceDto> getAllServices(@RequestAttribute("tenantId") String tenantId,
                                           @RequestParam(value = "branchId", required = false) String branchId) {
        String tId = getRequiredTenantId(tenantId);
        String niche = null;
        if (branchId != null && !branchId.isEmpty()) {
            niche = branchService.getBranches(tId).stream()
                    .filter(b -> b.getId().equals(branchId))
                    .map(Branch::getNiche)
                    .findFirst()
                    .orElse(null);
        }
        return appServiceService.getServicesByNiche(tId, niche)
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
                    return ResponseEntity.ok(Map.of("photoData", photoDataBase64 != null ? photoDataBase64 : ""));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resources")
    public List<ResourceDto> getResources(@RequestAttribute("tenantId") String tenantId,
                                          @RequestParam(value = "branchId", required = false) String branchId) {
        return resourceService.getResources(getRequiredTenantId(tenantId), branchId)
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/resources/{id}/photo")
    public ResponseEntity<Map<String, String>> getResourcePhoto(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return resourceService.getAllResources(getRequiredTenantId(tenantId)).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .map(resource -> {
                    String photoDataBase64 = null;
                    if (resource.getPhotoData() != null && resource.getPhotoData().length > 0) {
                        photoDataBase64 = Base64.getEncoder().encodeToString(resource.getPhotoData());
                    }
                    return ResponseEntity.ok(Map.of("photoData", photoDataBase64 != null ? photoDataBase64 : ""));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointments/day")
    public List<AppointmentDto> getAppointmentsForDay(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "branchId", required = false) String branchId,
            Authentication authentication) {
        
        String tId = getRequiredTenantId(tenantId);
        List<Appointment> appointments = scheduleService.getAppointmentsForDay(date, tId, branchId);
        
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        String clientContactId = currentUser != null ? currentUser.getContactId() : null;

        return appointments.stream().map(appt -> {
            AppointmentDto dto = DtoMapper.toDto(appt);
            if (clientContactId == null || !clientContactId.equals(appt.getContactId())) {
                dto.setClientName("Занято");
                dto.setClientPhone(null);
                dto.setComment(null);
                dto.setService("Занято");
                dto.setReferenceTag(null);
            } else {
                dto.setClientName("Ваша запись");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping("/appointments")
    public AppointmentDto createAppointment(@RequestBody AppointmentDto appointmentDto, @RequestAttribute("tenantId") String tenantId, Authentication authentication) {
        String tId = getRequiredTenantId(tenantId);
        
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        
        String contactId = currentUser.getContactId();
        Contact contact = null;
        if (contactId != null) {
            contact = contactService.getContactById(contactId).orElse(null);
        }
        
        if (contact == null) {
            contact = new Contact();
            contact.setName(currentUserEmail.split("@")[0]);
            contact.setEmail(currentUserEmail);
            contact.setTenantId(tId);
            contact = contactService.addContact(contact, tId);
            
            currentUser.setContactId(contact.getId());
            userRepository.save(currentUser);
        }
        
        appointmentDto.setContactId(contact.getId());
        appointmentDto.setClientName(contact.getName());
        if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
            appointmentDto.setClientPhone(contact.getPhones().get(0));
        }
        
        Appointment appointment = DtoMapper.toEntity(appointmentDto, tId);
        return DtoMapper.toDto(scheduleService.addAppointment(appointment));
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        
        Appointment appointment = scheduleService.getAppointmentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        if (currentUser.getContactId() == null || !currentUser.getContactId().equals(appointment.getContactId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы можете отменять только свои записи");
        }
        
        scheduleService.deleteAppointment(id);
        return ResponseEntity.ok().build();
    }
}
