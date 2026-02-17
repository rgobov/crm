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
        return incomingSecret != null && incomingSecret.equals(internalSecret);
    }

    @GetMapping("/qr")
    public ResponseEntity<?> getQr(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String qrLink = clientManager.getQrLink(tenantId);
        if (qrLink != null && !qrLink.isEmpty()) {
            String base64Image = qrCodeService.generateQrBase64(qrLink);
            return ResponseEntity.ok(Map.of("status", "WAITING_QR", "qrCode", base64Image));
        }
        
        if (clientManager.isSessionActive(tenantId)) {
            return ResponseEntity.ok(Map.of("status", "CONNECTED", "qrCode", ""));
        }

        return ResponseEntity.ok(Map.of("status", "INITIALIZING", "qrCode", ""));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("🚀 Manual connect request for tenant: {}", tenantId);
        clientManager.getClient(tenantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteSession(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam String tenantId) {
        
        if (!isAuthorized(secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("🗑 Request to delete session for tenant: {}", tenantId);
        clientManager.deleteSession(tenantId);
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

        if (tenantId == null || phone == null || text == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("status", "FAILED", "error", "Missing parameters")));
        }

        return clientManager.sendMessageByPhone(tenantId, phone, text)
                .thenApply(v -> ResponseEntity.ok(Map.of("status", "SUCCESS")))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "FAILED", "error", ex.getMessage())));
    }
}
