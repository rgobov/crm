package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class TemplateEngineService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public String process(String template, Appointment appointment) {
        if (template == null || appointment == null) return "";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{client}", appointment.getClientName());
        placeholders.put("{service}", appointment.getService());
        
        if (appointment.getStartTime() != null) {
            placeholders.put("{date}", appointment.getStartTime().format(DATE_FORMAT));
            placeholders.put("{time}", appointment.getStartTime().format(TIME_FORMAT));
        }

        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }
}
