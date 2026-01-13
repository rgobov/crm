package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public List<Contact> getAllContacts(@RequestAttribute("tenantId") String tenantId, @RequestParam(required = false) String query) {
        return contactService.getAllContacts(tenantId, query);
    }

    // --- НОВОЕ: Эндпоинт для подсчета клиентов ---
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
