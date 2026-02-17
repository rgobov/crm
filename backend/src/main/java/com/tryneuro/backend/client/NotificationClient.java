package com.tryneuro.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${services.notifications.url}")
public interface NotificationClient {

    @PostMapping("/api/telegram/send-by-phone")
    Map<String, String> sendTelegramMessage(@RequestBody Map<String, String> request);

    @GetMapping("/api/telegram/qr")
    Map<String, String> getQrStatus(@RequestParam("tenantId") String tenantId);

    @DeleteMapping("/api/telegram/session")
    void disconnect(@RequestParam("tenantId") String tenantId);
    
    @PostMapping("/api/telegram/connect")
    void connect(@RequestParam("tenantId") String tenantId);
}
