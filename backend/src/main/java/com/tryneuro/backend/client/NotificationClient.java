package com.tryneuro.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${services.notifications.url}")
public interface NotificationClient {

    @PostMapping("/api/telegram/send-by-phone")
    Map<String, String> sendTelegramMessage(@RequestBody Map<String, String> request);
}
