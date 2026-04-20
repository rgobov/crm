package com.tryneuro.notifications.telegram.controller;

import com.tryneuro.notifications.telegram.service.TelegramClientManager;
import com.tryneuro.notifications.telegram.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TelegramController {

    private final TelegramClientManager clientManager;
    private final QrCodeService qrCodeService;

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    private boolean isAuthorized(String incomingSecret) {
        return internalSecret.equals(incomingSecret);
    }

    @GetMapping("/qr")
    public ResponseEntity<?> getQr(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String status = clientManager.getExtendedStatus(tenantId);
        String qrLink = clientManager.getQrLink(tenantId);

        if (qrLink != null && !qrLink.isEmpty()) {
            try {
                String base64Image = qrCodeService.generateQrBase64(qrLink);
                return ResponseEntity.ok(Map.of("status", "WAITING_QR", "qrCode", base64Image));
            } catch (Exception e) {
                log.error("Failed to generate QR for tenant {}: {}", tenantId, e.getMessage());
            }
        }
        
        if ("CONNECTED".equals(status)) {
            return ResponseEntity.ok(Map.of("status", "CONNECTED", "qrCode", ""));
        }

        return ResponseEntity.ok(Map.of("status", status, "qrCode", ""));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("🔄 Reconnect (atomic) request for tenant: {}", tenantId);
        clientManager.initiateReconnect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    public ResponseEntity<?> checkPassword(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody Map<String, String> body) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String tenantId = body.get("tenantId");
        String password = body.get("password");
        
        if (tenantId == null || password == null) return ResponseEntity.badRequest().build();

        clientManager.checkPassword(tenantId, password);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteSession(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {

        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("🗑 Request to delete session for tenant: {}", tenantId);
        clientManager.forceDisconnect(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel-qr")
    public ResponseEntity<?> cancelQrGeneration(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {

        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("🚫 Request to cancel QR generation for tenant: {}", tenantId);
        clientManager.cancelQrGeneration(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-by-phone")
    public CompletableFuture<ResponseEntity<Map<String, String>>> sendByPhone(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody Map<String, String> request) {
        
        if (!isAuthorized(secret)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "FORBIDDEN")));
        }

        String tenantId = request.get("tenantId");
        String phone = request.get("phone");
        String text = request.get("text");
        // Извлекаем имя клиента, если оно передано бэкендом
        String name = request.getOrDefault("name", "Клиент CRM");

        return clientManager.sendMessageByPhone(tenantId, phone, name, text)
                .thenApply(v -> ResponseEntity.ok(Map.of("status", "SUCCESS")))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "FAILED", "error", ex.getMessage())));
    }
}
