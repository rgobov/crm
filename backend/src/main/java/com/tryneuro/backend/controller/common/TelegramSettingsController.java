package com.tryneuro.backend.controller.common;

import com.tryneuro.backend.model.TelegramSettings;
import com.tryneuro.backend.service.TelegramSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/telegram")
@RequiredArgsConstructor
public class TelegramSettingsController {

    private final TelegramSettingsService telegramSettingsService;
    private final HttpServletRequest request; // Внедряем HttpServletRequest для доступа к атрибутам

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        // Извлекаем tenantId, который JwtAuthenticationFilter положил в атрибуты запроса
        String tenantId = (String) request.getAttribute("tenantId");
        
        TelegramSettings settings = telegramSettingsService.getSettings(tenantId);
        if (settings != null && settings.isActive()) {
            return ResponseEntity.ok(Map.of(
                "status", "CONNECTED", 
                "phone", settings.getConnectedPhone() != null ? settings.getConnectedPhone() : ""
            ));
        }
        return ResponseEntity.ok(telegramSettingsService.getRealTimeStatus(tenantId));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect() {
        String tenantId = (String) request.getAttribute("tenantId");
        telegramSettingsService.connect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect() {
        String tenantId = (String) request.getAttribute("tenantId");
        telegramSettingsService.disconnect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/sync")
    public ResponseEntity<?> syncStatus(
            @RequestHeader(value = "X-Internal-Secret", required = false) String incomingSecret,
            @RequestBody Map<String, Object> data) {
        
        if (incomingSecret == null || !incomingSecret.equals(internalSecret)) {
            log.warn("Unauthorized internal sync attempt with secret: {}", incomingSecret);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String tenantId = (String) data.get("tenantId");
        String status = (String) data.get("status");
        telegramSettingsService.updateStatus(tenantId, status);
        return ResponseEntity.ok().build();
    }
}
