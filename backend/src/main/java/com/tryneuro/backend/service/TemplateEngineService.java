package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateEngineService {

    private final BranchRepository branchRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public String process(String template, Appointment appointment) {
        if (template == null || appointment == null) return "";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{client}", appointment.getClientName());
        placeholders.put("{service}", appointment.getService());
        
        if (appointment.getStartTime() != null) {
            // ОПРЕДЕЛЯЕМ ЛОКАЛЬНОЕ ВРЕМЯ ФИЛИАЛА
            ZonedDateTime branchLocalTime = getBranchLocalTime(appointment);
            
            placeholders.put("{date}", branchLocalTime.format(DATE_FORMAT));
            placeholders.put("{time}", branchLocalTime.format(TIME_FORMAT));
        }

        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }

    /**
     * Конвертирует системное время записи в локальное время филиала
     */
    private ZonedDateTime getBranchLocalTime(Appointment appointment) {
        ZoneId zoneId;
        try {
            // Пытаемся найти филиал и его часовой пояс
            zoneId = branchRepository.findById(appointment.getBranchId())
                    .map(Branch::getTimezone)
                    .map(ZoneId::of)
                    .orElse(ZoneId.of("Europe/Moscow")); // Дефолт, если не нашли
        } catch (Exception e) {
            log.warn("Could not determine timezone for branch {}. Using Moscow default.", appointment.getBranchId());
            zoneId = ZoneId.of("Europe/Moscow");
        }

        // Переводим OffsetDateTime в ZonedDateTime конкретного филиала
        return appointment.getStartTime().atZoneSameInstant(zoneId);
    }
}
