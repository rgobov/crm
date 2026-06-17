package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    List<NotificationTemplate> findByTenantId(String tenantId);
    Optional<NotificationTemplate> findByTenantIdAndType(String tenantId, String type);
}
