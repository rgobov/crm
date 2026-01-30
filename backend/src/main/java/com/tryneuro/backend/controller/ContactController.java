package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public Page<Contact> getAllContacts(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contactService.getContactsPaged(tenantId, query, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> getContactById(@PathVariable String id) {
        return contactService.getContactById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public long getContactsCount(@RequestAttribute("tenantId") String tenantId) {
        // ДИАГНОСТИКА: Печатаем tenantId из запроса
        System.out.println(">>> DEBUG: API /contacts/count called with tenantId: [" + tenantId + "]");
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
