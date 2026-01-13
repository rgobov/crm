package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.repository.WappiSettingsRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WappiService {

    private final WappiSettingsRepository settingsRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WappiSettings saveSettings(String tenantId, WappiSettings newSettings) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElse(new WappiSettings());
        
        settings.setTenantId(tenantId);
        settings.setApiKey(newSettings.getApiKey());
        settings.setProfileId(newSettings.getProfileId());
        settings.setEnabled(newSettings.isEnabled());
        settings.setReminderTemplate(newSettings.getReminderTemplate());
        settings.setMessengerType(newSettings.getMessengerType());
        settings.setLeadTimeMinutes(newSettings.getLeadTimeMinutes());
        
        return settingsRepository.save(settings);
    }

    public WappiSettings getSettings(String tenantId) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElse(new WappiSettings());
        settings.setTenantId(tenantId);
        return settings;
    }

    public void sendReminder(Appointment appointment, Contact contact) {
        WappiSettings settings = settingsRepository.findByTenantId(appointment.getTenantId())
                .orElse(null);

        if (settings == null || !settings.isEnabled() || settings.getApiKey() == null) {
            return;
        }

        String message = buildMessage(appointment, contact, settings.getReminderTemplate());

        if (contact.getPhones().isEmpty()) return;
        String phone = contact.getPhones().get(0).replace("+", "");

        // Отправляем сообщение с кнопками
        sendButtonsToWappi(settings, phone, message);
    }

    private String buildMessage(Appointment appointment, Contact contact, String template) {
        String masterName = staffMemberRepository.findById(appointment.getStaffMemberId())
                .map(StaffMember::getName).orElse("Специалист");

        return template
                .replace("{name}", contact.getName())
                .replace("{service}", appointment.getService())
                .replace("{date}", appointment.getDate().format(DateTimeFormatter.ofPattern("dd.MM")))
                .replace("{time}", appointment.getTime().toString())
                .replace("{master}", masterName);
    }

    private void sendButtonsToWappi(WappiSettings settings, String phone, String text) {
        String url = "https://api.wappi.pro/v1/send/buttons"; // Используем эндпоинт для кнопок
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", settings.getApiKey());

        // Создаем кнопки
        List<Map<String, String>> buttons = new ArrayList<>();
        buttons.add(Map.of("id", "confirm", "text", "✅ Подтверждаю"));
        buttons.add(Map.of("id", "cancel", "text", "❌ Отмена/Перенос"));

        Map<String, Object> body = new HashMap<>();
        body.put("recipient", phone);
        body.put("body", text);
        body.put("profile_id", settings.getProfileId());
        body.put("buttons", buttons);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("Reminder with buttons sent to " + phone);
        } catch (Exception e) {
            System.err.println("Wappi error: " + e.getMessage());
        }
    }
}
