package com.tryneuro.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${services.notifications.url}")
public interface NotificationClient {

    @PostMapping("/api/telegram/send-by-phone")
    Map<String, String> sendTelegramMessage(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestBody Map<String, String> request
    );

    @GetMapping("/api/telegram/qr")
    Map<String, String> getQrStatus(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestParam("tenantId") String tenantId
    );

    @DeleteMapping("/api/telegram/session")
    void disconnect(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestParam("tenantId") String tenantId
    );
    
    @PostMapping("/api/telegram/connect")
    void connect(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestParam("tenantId") String tenantId
    );

    @PostMapping("/api/telegram/password")
    void checkPassword(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestBody Map<String, String> request
    );

    @PostMapping("/api/telegram/cancel-qr")
    void cancelQrGeneration(
        @RequestHeader("X-Internal-Secret") String secret,
        @RequestParam("tenantId") String tenantId
    );
}
