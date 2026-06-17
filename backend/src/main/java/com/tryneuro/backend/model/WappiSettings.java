package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wappi_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WappiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "profile_id")
    private String profileId;

    @Column(name = "is_enabled")
    private boolean isEnabled = false;

    @Column(name = "reminder_template", columnDefinition = "TEXT")
    private String reminderTemplate = "👋 Здравствуйте, {name}! \n\nНапоминаем о вашей записи на услугу: {service}.\n🗓 Дата: {date}\n⏰ Время: {time}\n👤 Мастер: {master}\n\nПодтверждаете свой визит?";

    @Column(name = "messenger_type")
    private String messengerType = "TELEGRAM";

    // --- ВОЗВРАЩАЕМ МИНУТЫ ДЛЯ ТОЧНОСТИ ---
    @Column(name = "lead_time_minutes")
    private int leadTimeMinutes = 1440; 
}
