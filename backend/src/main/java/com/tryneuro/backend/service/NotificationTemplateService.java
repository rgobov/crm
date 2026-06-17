package com.tryneuro.backend.service;

import com.tryneuro.backend.model.NotificationTemplate;
import com.tryneuro.backend.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    // ОБНОВЛЕННЫЕ ДЕФОЛТЫ (соответствуют новой логике)
    private static final Map<String, String> DEFAULT_TEMPLATES = Map.of(
        "REMINDER", "Здравствуйте, {client}! Напоминаем о вашей записи на {service}: {date} в {time}.",
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

    public NotificationTemplate getTemplateByType(String tenantId, String type) {
        return repository.findByTenantIdAndType(tenantId, type)
                .orElseGet(() -> {
                    NotificationTemplate t = new NotificationTemplate();
                    t.setTenantId(tenantId);
                    t.setType(type);
                    t.setContent(DEFAULT_TEMPLATES.getOrDefault(type, ""));
                    t.setLeadTimeHours(24);
                    return t;
                });
    }

    public String getTemplateContent(String tenantId, String type) {
        return repository.findByTenantIdAndType(tenantId, type)
                .map(NotificationTemplate::getContent)
                .orElse(DEFAULT_TEMPLATES.getOrDefault(type, ""));
    }
}
