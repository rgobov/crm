package com.tryneuro.backend.controller.flutter;

import com.tryneuro.backend.dto.ContactDto;
import com.tryneuro.backend.dto.DtoMapper;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.service.ContactService;
import com.tryneuro.backend.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;
    private final ScheduleService scheduleService;

    @Autowired
    public ContactController(ContactService contactService, ScheduleService scheduleService) {
        this.contactService = contactService;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public Page<ContactDto> getAllContacts(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "false") boolean showAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return contactService.getContactsPaged(tenantId, query, showAll, page, size)
                .map(DtoMapper::toDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactDto> getContactById(@PathVariable String id) {
        return contactService.getContactById(id)
                .map(DtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/appointments")
    public List<Appointment> getContactAppointments(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String id) {
        // Appointments are still using Entity here, but we should probably DTO-ify them too if needed.
        // For now, keeping as is or using toDto if AppointmentDto is ready.
        return scheduleService.getAppointmentsForContact(id, tenantId);
    }

    @GetMapping("/count")
    public long getContactsCount(@RequestAttribute("tenantId") String tenantId) {
        return contactService.countContacts(tenantId);
    }

    @GetMapping("/by-phone")
    public ResponseEntity<ContactDto> findByPhone(@RequestAttribute("tenantId") String tenantId, @RequestParam String phone) {
        return contactService.findContactByPhone(phone, tenantId)
                .map(DtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ContactDto addContact(@RequestBody ContactDto contactDto, @RequestAttribute("tenantId") String tenantId) {
        Contact contact = DtoMapper.toEntity(contactDto, tenantId);
        return DtoMapper.toDto(contactService.addContact(contact, tenantId));
    }

    @PutMapping("/{id}")
    public ContactDto updateContact(@PathVariable String id, @RequestBody ContactDto contactDto, @RequestAttribute("tenantId") String tenantId) {
        Contact contact = DtoMapper.toEntity(contactDto, tenantId);
        return DtoMapper.toDto(contactService.updateContact(id, contact, tenantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok().build();
    }
}
