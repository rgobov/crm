package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {

    List<Contact> findByTenantId(String tenantId);

    // ИСПРАВЛЕНО: Полная поддержка регистронезависимого поиска по Имени, Телефону и Тегам
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND (" +
           "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "array_to_string(phones, ',') LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(array_to_string(tags, ',')) LIKE LOWER(CONCAT('%', :query, '%')))",
           countQuery = "SELECT count(*) FROM contacts WHERE tenant_id = :tenantId AND (" +
           "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "array_to_string(phones, ',') LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(array_to_string(tags, ',')) LIKE LOWER(CONCAT('%', :query, '%')))",
           nativeQuery = true)
    Page<Contact> searchContacts(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

    Page<Contact> findByTenantId(String tenantId, Pageable pageable);

    @Query(value = "SELECT DISTINCT c.* FROM contacts c " +
           "JOIN appointments a ON a.contact_id = c.id " +
           "WHERE c.tenant_id = :tenantId AND CAST(a.start_time AS date) = :date",
           countQuery = "SELECT count(DISTINCT c.id) FROM contacts c " +
           "JOIN appointments a ON a.contact_id = c.id " +
           "WHERE c.tenant_id = :tenantId AND CAST(a.start_time AS date) = :date",
           nativeQuery = true)
    Page<Contact> findByAppointmentDate(@Param("tenantId") String tenantId, @Param("date") LocalDate date, Pageable pageable);

    long countByTenantId(String tenantId);

    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND " +
           "EXISTS (SELECT 1 FROM unnest(phones) AS p WHERE REPLACE(REPLACE(REPLACE(REPLACE(p, '+', ''), '(', ''), ')', ''), '-', '') = :phone) " +
           "LIMIT 1",
           nativeQuery = true)
    Optional<Contact> findByCleanPhone(@Param("phone") String phone, @Param("tenantId") String tenantId);

    Optional<Contact> findByTelegramId(Long telegramId);
}
