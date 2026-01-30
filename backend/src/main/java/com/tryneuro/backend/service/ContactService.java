package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ContactService {
    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    // ОБНОВЛЕНО: Добавлен параметр showAll
    public Page<Contact> getContactsPaged(String tenantId, String query, boolean showAll, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        // 1. Поиск всегда имеет приоритет (от 3-х символов)
        if (query != null && query.trim().length() >= 3) {
            return contactRepository.searchContacts(tenantId, query.trim(), pageable);
        }

        // 2. Если поиска нет, смотрим на переключатель
        if (showAll) {
            // Показываем абсолютно всех клиентов компании
            return contactRepository.findByTenantId(tenantId, pageable);
        }

        // 3. По умолчанию (или если выбрано "Сегодня") - только клиенты с записями
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

    public Contact updateContact(String id, Contact contact, String tenantId) {
        contact.setId(id);
        contact.setTenantId(tenantId);
        return contactRepository.save(contact);
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }
}
