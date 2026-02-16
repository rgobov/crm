package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.TelegramSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramSettingsRepository extends JpaRepository<TelegramSettings, String> {
}
