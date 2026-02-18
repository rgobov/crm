package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository, AppointmentRepository appointmentRepository) {
        this.contactRepository = contactRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Page<Contact> getContactsPaged(String tenantId, String query, boolean showAll, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        if (query != null && query.trim().length() >= 3) {
            return contactRepository.searchContacts(tenantId, query.trim(), pageable);
        }
        if (showAll) {
            return contactRepository.findByTenantId(tenantId, pageable);
        }
        return contactRepository.findByAppointmentDate(tenantId, LocalDate.now(), pageable);
    }

    public long countContacts(String tenantId) {
        return contactRepository.countByTenantId(tenantId);
    }

    public Optional<Contact> getContactById(String id) {
        return contactRepository.findById(id);
    }

    public Optional<Contact> findContactByPhone(String phone, String tenantId) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return contactRepository.findByCleanPhone(cleanPhone, tenantId);
    }

    public Contact addContact(Contact contact, String tenantId) {
        for (String phone : contact.getPhones()) {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            Optional<Contact> existing = contactRepository.findByCleanPhone(cleanPhone, tenantId);
            if (existing.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Клиент с номером " + phone + " уже существует.");
            }
        }
        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact updateContact(String id, Contact contact, String tenantId) {
        contact.setId(id);
        contact.setTenantId(tenantId);
        Contact saved = contactRepository.save(contact);
        
        // СИНХРОНИЗАЦИЯ: Обновляем имя клиента во всех его записях
        appointmentRepository.updateClientNameForContact(id, saved.getName(), tenantId);
        
        return saved;
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }
}
