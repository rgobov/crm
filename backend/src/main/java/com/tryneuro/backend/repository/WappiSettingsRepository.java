package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.WappiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WappiSettingsRepository extends JpaRepository<WappiSettings, String> {
    Optional<WappiSettings> findByTenantId(String tenantId);
    
    // --- НОВОЕ: Поиск компании по идентификатору Wappi профиля ---
    Optional<WappiSettings> findByProfileId(String profileId);
}
