package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {
    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public List<Contact> getAllContacts(String tenantId) {
        return contactRepository.findByTenantId(tenantId);
    }

    // Новый метод
    public Optional<Contact> findContactByPhone(String phone, String tenantId) {
        return contactRepository.findByPhoneAndTenantId(phone, tenantId);
    }

    public Contact addContact(Contact contact, String tenantId) {
        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    public Contact updateContact(String id, Contact contact, String tenantId) {
        contact.setId(id);
        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }
}
