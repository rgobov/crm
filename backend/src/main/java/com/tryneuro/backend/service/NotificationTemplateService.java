package com.tryneuro.backend.service;

import com.tryneuro.backend.model.NotificationTemplate;
import com.tryneuro.backend.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    // Тексты по умолчанию, если компания еще не создала свои шаблоны
    private static final Map<String, String> DEFAULT_TEMPLATES = Map.of(
        "APPOINTMENT_CONFIRMATION", "Здравствуйте, {client}! Вы записаны на услугу {service}. Ждем вас {date} в {time}.",
        "REMINDER_2_HOURS", "Напоминаем: у вас сегодня визит в {time} на услугу {service}. До встречи!",
        "APPOINTMENT_CANCELLED", "Ваша запись на {date} в {time} ({service}) была отменена."
    );

    public List<NotificationTemplate> getAllTemplates(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Transactional
    public NotificationTemplate saveTemplate(String tenantId, NotificationTemplate template) {
        template.setTenantId(tenantId);
        return repository.save(template);
    }

    public String getTemplateContent(String tenantId, String type) {
        return repository.findByTenantIdAndType(tenantId, type)
                .map(NotificationTemplate::getContent)
                .orElse(DEFAULT_TEMPLATES.getOrDefault(type, ""));
    }
}
