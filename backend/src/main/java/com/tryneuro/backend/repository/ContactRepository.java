package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Contact; // Добавляем этот импорт
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByTenantId(String tenantId);
    
    Optional<Contact> findByPhoneAndTenantId(String phone, String tenantId);
}
