package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final AppointmentRepository appointmentRepository;

    public Page<Contact> getContactsPaged(String tenantId, String query, boolean showAll, int page, int size) {
        if (query != null && !query.isEmpty()) {
            return contactRepository.searchContacts(tenantId, query, PageRequest.of(page, size));
        }
        return contactRepository.findByTenantId(tenantId, PageRequest.of(page, size));
    }

    public Optional<Contact> getContactById(String id) {
        return contactRepository.findById(id);
    }

    public Optional<Contact> findContactByPhone(String phone, String tenantId) {
        if (phone == null || phone.isEmpty()) return Optional.empty();
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 11 && cleanPhone.startsWith("8")) {
            cleanPhone = "7" + cleanPhone.substring(1);
        }
        return contactRepository.findByCleanPhone(cleanPhone, tenantId);
    }

    public Contact addContact(Contact contact, String tenantId) {
        contact.setTenantId(tenantId);
        if (contact.getTags() == null) contact.setTags(new ArrayList<>());
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact updateContact(String id, Contact details, String tenantId) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        
        String oldName = contact.getName();
        String newName = details.getName();

        contact.setName(newName);
        contact.setPhones(details.getPhones());
        contact.setEmail(details.getEmail());
        contact.setNotes(details.getNotes());
        contact.setTags(details.getTags());
        
        Contact saved = contactRepository.save(contact);

        // СИНХРОНИЗАЦИЯ: Если имя изменилось, обновляем его во всех записях (таймлайн)
        if (!newName.equals(oldName)) {
            appointmentRepository.updateClientNameForContact(id, newName, tenantId);
        }

        return saved;
    }

    @Transactional
    public void addTagIfMissing(String contactId, String newTag) {
        if (newTag == null || newTag.trim().isEmpty()) return;
        contactRepository.findById(contactId).ifPresent(contact -> {
            List<String> tags = contact.getTags();
            if (tags == null) tags = new ArrayList<>();
            String trimmedTag = newTag.trim();
            boolean exists = tags.stream().anyMatch(t -> t.equalsIgnoreCase(trimmedTag));
            if (!exists) {
                tags.add(trimmedTag);
                contact.setTags(tags);
                contactRepository.save(contact);
            }
        });
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }

    public long countContacts(String tenantId) {
        return contactRepository.countByTenantId(tenantId);
    }

    public Page<Contact> findByAppointmentDate(String tenantId, LocalDate date, int page, int size) {
        return contactRepository.findByAppointmentDate(tenantId, date, PageRequest.of(page, size));
    }
}
