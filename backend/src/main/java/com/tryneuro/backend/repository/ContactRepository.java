package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByTenantId(String tenantId);
    
    // Поиск по конкретному номеру телефона в массиве
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND :phone = ANY(phones)", nativeQuery = true)
    Optional<Contact> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") String tenantId);

    // Поиск по части имени или по любому из номеров в массиве
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND (" +
           "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(phones) AS p WHERE p LIKE CONCAT('%', :query, '%')))", nativeQuery = true)
    List<Contact> searchContacts(@Param("tenantId") String tenantId, @Param("query") String query);
}
