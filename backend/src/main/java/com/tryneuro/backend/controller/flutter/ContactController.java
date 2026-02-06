package com.tryneuro.backend.controller.flutter;

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
    public Page<Contact> getAllContacts(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "false") boolean showAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return contactService.getContactsPaged(tenantId, query, showAll, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> getContactById(@PathVariable String id) {
        return contactService.getContactById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/appointments")
    public List<Appointment> getContactAppointments(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String id) {
        return scheduleService.getAppointmentsForContact(id, tenantId);
    }

    @GetMapping("/count")
    public long getContactsCount(@RequestAttribute("tenantId") String tenantId) {
        return contactService.countContacts(tenantId);
    }

    @GetMapping("/by-phone")
    public ResponseEntity<Contact> findByPhone(@RequestAttribute("tenantId") String tenantId, @RequestParam String phone) {
        return contactService.findContactByPhone(phone, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Contact addContact(@RequestBody Contact contact, @RequestAttribute("tenantId") String tenantId) {
        return contactService.addContact(contact, tenantId);
    }

    @PutMapping("/{id}")
    public Contact updateContact(@PathVariable String id, @RequestBody Contact contact, @RequestAttribute("tenantId") String tenantId) {
        return contactService.updateContact(id, contact, tenantId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok().build();
    }
}
