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
        if (qrLink != null && !qrLink.isEmpty()) {
            String base64Image = qrCodeService.generateQrBase64(qrLink);
            return Map.of("status", "WAITING_QR", "qrCode", base64Image);
        }
        return Map.of("status", "CONNECTED", "qrCode", "");
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
