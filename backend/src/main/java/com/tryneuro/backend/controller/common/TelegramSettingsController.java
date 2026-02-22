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
    private final HttpServletRequest request;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        String tenantId = (String) request.getAttribute("tenantId");
        
        // Только запрашиваем реальное состояние у микросервиса
        Map<String, String> realTimeStatus = telegramSettingsService.getRealTimeStatus(tenantId);
        String status = realTimeStatus.get("status");

        // Если реально подключено, обогащаем данными из БД
        if ("CONNECTED".equals(status)) {
            TelegramSettings settings = telegramSettingsService.getSettings(tenantId);
            return ResponseEntity.ok(Map.of(
                "status", "CONNECTED",
                "phone", (settings != null && settings.getConnectedPhone() != null) ? settings.getConnectedPhone() : ""
            ));
        }

        // Возвращаем как есть (WAITING_QR, DISCONNECTED, etc), НЕ вызывая updateStatus
        return ResponseEntity.ok(realTimeStatus);
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect() {
        String tenantId = (String) request.getAttribute("tenantId");
        log.info("🚀 Received connect request for tenant: {}", tenantId);
        telegramSettingsService.connect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect() {
        String tenantId = (String) request.getAttribute("tenantId");
        log.info("🗑 Received disconnect request for tenant: {}", tenantId);
        telegramSettingsService.disconnect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/sync")
    public ResponseEntity<?> syncStatus(
            @RequestHeader(value = "X-Internal-Secret", required = false) String incomingSecret,
            @RequestBody Map<String, Object> data) {
        
        if (incomingSecret == null || !incomingSecret.equals(internalSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String tenantId = (String) data.get("tenantId");
        String status = (String) data.get("status");
        
        // Сюда приходят реальные обновления статуса. Только здесь вызываем обновление и рассылку в WS.
        telegramSettingsService.updateStatus(tenantId, status);
        return ResponseEntity.ok().build();
    }
}
