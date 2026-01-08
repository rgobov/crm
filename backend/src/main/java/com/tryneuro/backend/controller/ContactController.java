package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public List<Contact> getAllContacts(@AuthenticationPrincipal User user, 
                                        @RequestParam(required = false) String query) {
        return contactService.getAllContacts(user.getTenantId(), query);
    }

    @GetMapping("/by-phone")
    public ResponseEntity<Contact> getContactByPhone(@RequestParam String phone, @AuthenticationPrincipal User user) {
        return contactService.findContactByPhone(phone, user.getTenantId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Contact createContact(@RequestBody Contact contact, @AuthenticationPrincipal User user) {
        return contactService.addContact(contact, user.getTenantId());
    }

    @PutMapping("/{id}")
    public Contact updateContact(@PathVariable String id, @RequestBody Contact contact, @AuthenticationPrincipal User user) {
        return contactService.updateContact(id, contact, user.getTenantId());
    }

    @DeleteMapping("/{id}")
    public void deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
    }
}
