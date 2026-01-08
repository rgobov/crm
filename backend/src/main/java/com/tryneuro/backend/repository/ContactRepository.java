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
    
    Optional<Contact> findByPhoneAndTenantId(String phone, String tenantId);

    // Поиск по имени ИЛИ телефону (игнорируя регистр и небуквенные символы в телефоне для гибкости)
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.phone LIKE CONCAT('%', :query, '%'))")
    List<Contact> searchContacts(@Param("tenantId") String tenantId, @Param("query") String query);
}
