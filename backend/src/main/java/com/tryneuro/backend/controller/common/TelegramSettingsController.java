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
        Map<String, String> realTimeStatus = telegramSettingsService.getRealTimeStatus(tenantId);
        String status = realTimeStatus.get("status");

        if ("CONNECTED".equals(status)) {
            TelegramSettings settings = telegramSettingsService.getSettings(tenantId);
            return ResponseEntity.ok(Map.of(
                "status", "CONNECTED",
                "phone", (settings != null && settings.getConnectedPhone() != null) ? settings.getConnectedPhone() : ""
            ));
        }

        return ResponseEntity.ok(realTimeStatus);
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect() {
        String tenantId = (String) request.getAttribute("tenantId");
        log.info("🚀 Received connect request for tenant: {}", tenantId);
        telegramSettingsService.connect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    public ResponseEntity<?> checkPassword(@RequestBody Map<String, String> body) {
        String tenantId = (String) request.getAttribute("tenantId");
        String password = body.get("password");
        log.info("🔑 Received password request for tenant: {}", tenantId);
        telegramSettingsService.checkPassword(tenantId, password);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        String tenantId = (String) request.getAttribute("tenantId");
        String phoneNumber = body.get("phoneNumber");
        log.info("📱 Received send code request for tenant: {}, phone: {}", tenantId, phoneNumber);
        Map<String, String> result = telegramSettingsService.sendCode(tenantId, phoneNumber);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> body) {
        String tenantId = (String) request.getAttribute("tenantId");
        String code = body.get("code");
        log.info("✅ Received sign in request for tenant: {}", tenantId);
        Map<String, String> result = telegramSettingsService.signIn(tenantId, code);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cancel-qr")
    public ResponseEntity<?> cancelQrGeneration() {
        String tenantId = (String) request.getAttribute("tenantId");
        log.info("🚫 Received cancel QR request for tenant: {}", tenantId);
        telegramSettingsService.cancelQrGeneration(tenantId);
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
        
        // ФИКС: Передаем всю карту data (Map<String, Object>), как и требует сервис
        telegramSettingsService.updateStatus(tenantId, data);
        return ResponseEntity.ok().build();
    }
}
