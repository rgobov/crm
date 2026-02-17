package com.tryneuro.notifications.telegram.controller;

import com.tryneuro.notifications.telegram.service.TelegramClientManager;
import com.tryneuro.notifications.telegram.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        String status = "WAITING_QR";
        String base64Image = "";

        if (qrLink != null && !qrLink.isEmpty()) {
            log.info("Generating QR image for link: {}", qrLink);
            base64Image = qrCodeService.generateQrBase64(qrLink);
            if (base64Image == null || base64Image.isEmpty()) {
                log.error("QR image generation failed for tenant: {}", tenantId);
            }
        } else {
            status = "INITIALIZING";
        }

        return Map.of("status", status, "qrCode", base64Image != null ? base64Image : "");
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
    public ResponseEntity<?> sendByPhone(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String phone = request.get("phone");
        String text = request.get("text");

        if (tenantId == null || phone == null || text == null) {
            return ResponseEntity.badRequest().body("Missing parameters");
        }

        return ResponseEntity.ok(
            clientManager.sendMessageByPhone(tenantId, phone, text)
                .thenApply(v -> Map.of("status", "SUCCESS"))
                .exceptionally(ex -> Map.of("status", "FAILED", "error", ex.getMessage()))
        );
    }
}
