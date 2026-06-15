package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import com.tryneuro.backend.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ContactRepository contactRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public byte[] exportClientsToExcel(String tenantId, String query, boolean showAll) throws IOException {
        log.info("📊 Exporting clients to Excel for tenantId={}, query={}, showAll={}", tenantId, query, showAll);
        List<Contact> contacts;
        if (!showAll) {
            // "На сегодня"
            contacts = contactRepository.findByAppointmentDate(tenantId, LocalDate.now(), PageRequest.of(0, 100000)).getContent();
        } else if (query != null && !query.isEmpty()) {
            contacts = contactRepository.searchContacts(tenantId, query, PageRequest.of(0, 100000)).getContent();
        } else {
            contacts = contactRepository.findByTenantId(tenantId);
        }
        return ExcelExportUtil.exportClients(contacts);
    }

    @Transactional(readOnly = true)
    public byte[] exportAppointmentsToExcel(String tenantId, String contactId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("📊 Exporting appointments to Excel for tenantId={}, contactId={}, range [{} - {}]", tenantId, contactId, startDate, endDate);
        List<Appointment> appointments;
        if (contactId != null && !contactId.isEmpty()) {
            appointments = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
        } else {
            appointments = appointmentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate);
        }
        return ExcelExportUtil.exportAppointments(appointments);
    }
}
