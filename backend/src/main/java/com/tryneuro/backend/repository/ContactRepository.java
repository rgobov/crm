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
    
    // Поиск по конкретному номеру телефона в массиве (полное совпадение)
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND :phone = ANY(phones)", nativeQuery = true)
    Optional<Contact> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") String tenantId);

    /**
     * Продвинутый поиск:
     * 1. Ищет по части имени (регистронезависимо)
     * 2. Ищет по любому номеру в массиве, ПРЕДВАРИТЕЛЬНО ОЧИЩАЯ номера в базе от знака '+' для сравнения
     */
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND (" +
           "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(phones) AS p WHERE REPLACE(p, '+', '') LIKE CONCAT('%', :query, '%')))", 
           nativeQuery = true)
    List<Contact> searchContacts(@Param("tenantId") String tenantId, @Param("query") String query);

    // Специальный метод для поиска ОДНОГО контакта по очищенному номеру (для автоподстановки)
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND " +
           "EXISTS (SELECT 1 FROM unnest(phones) AS p WHERE REPLACE(p, '+', '') = :phone) LIMIT 1", 
           nativeQuery = true)
    Optional<Contact> findByCleanPhone(@Param("phone") String phone, @Param("tenantId") String tenantId);
}
