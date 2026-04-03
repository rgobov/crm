package com.tryneuro.notifications.telegram.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "backend-service", url = "${backend.url:http://localhost:8080}")
public interface BackendClient {

    @PostMapping("/api/admin/telegram/internal/sync")
    String syncStatus(
        @RequestHeader("X-Internal-Secret") String secret, 
        @RequestBody Map<String, Object> data
    );
}
