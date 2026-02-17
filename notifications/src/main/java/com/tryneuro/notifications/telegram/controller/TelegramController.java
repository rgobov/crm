package com.tryneuro.notifications.telegram.controller;

import com.tryneuro.notifications.telegram.service.TelegramClientManager;
import com.tryneuro.notifications.telegram.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/qr")
    public Map<String, String> getQr(@RequestParam String tenantId) {
        String qrLink = clientManager.getQrLink(tenantId);
        
        if (qrLink != null && !qrLink.isEmpty()) {
            String base64Image = qrCodeService.generateQrBase64(qrLink);
            return Map.of("status", "WAITING_QR", "qrCode", base64Image);
        }
        
        if (clientManager.isSessionActive(tenantId)) {
            return Map.of("status", "CONNECTED", "qrCode", "");
        }

        return Map.of("status", "INITIALIZING", "qrCode", "");
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestParam String tenantId) {
        log.info("🚀 Manual connect request for tenant: {}", tenantId);
        clientManager.getClient(tenantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteSession(@RequestParam String tenantId) {
        log.info("🗑 Request to delete session for tenant: {}", tenantId);
        clientManager.deleteSession(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-by-phone")
    public CompletableFuture<Map<String, String>> sendByPhone(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String phone = request.get("phone");
        String text = request.get("text");

        if (tenantId == null || phone == null || text == null) {
            return CompletableFuture.completedFuture(Map.of("status", "FAILED", "error", "Missing parameters"));
        }

        log.info("📧 Sending test message to {} for tenant {}", phone, tenantId);

        return clientManager.sendMessageByPhone(tenantId, phone, text)
                .thenApply(v -> Map.of("status", "SUCCESS"))
                .exceptionally(ex -> {
                    log.error("❌ Send error: {}", ex.getMessage());
                    return Map.of("status", "FAILED", "error", ex.getMessage());
                });
    }
}
