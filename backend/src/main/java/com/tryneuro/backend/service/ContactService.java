package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {
    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public List<Contact> getAllContacts(String tenantId, String query) {
        if (query != null && !query.isEmpty()) {
            return contactRepository.searchContacts(tenantId, query);
        }
        return contactRepository.findByTenantId(tenantId);
    }

    // --- НОВОЕ: Подсчет количества ---
    public long countContacts(String tenantId) {
        return contactRepository.countByTenantId(tenantId);
    }

    public Optional<Contact> findContactByPhone(String phone, String tenantId) {
        return contactRepository.findByCleanPhone(phone, tenantId);
    }

    public Contact addContact(Contact contact, String tenantId) {
        for (String phone : contact.getPhones()) {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            Optional<Contact> existing = contactRepository.findByCleanPhone(cleanPhone, tenantId);
            if (existing.isPresent()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "Клиент с номером " + phone + " уже существует: " + existing.get().getName()
                );
            }
        }

        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    public Contact updateContact(String id, Contact contact, String tenantId) {
        for (String phone : contact.getPhones()) {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            contactRepository.findByCleanPhone(cleanPhone, tenantId)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ResponseStatusException(
                            HttpStatus.CONFLICT, 
                            "Номер " + phone + " уже закреплен за другим клиентом"
                        );
                    }
                });
        }

        contact.setId(id);
        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }
}
