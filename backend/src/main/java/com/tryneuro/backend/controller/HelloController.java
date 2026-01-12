package com.tryneuro.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from TryNeuro Backend!";
    }

    // --- НОВЫЙ ЭНДПОИНТ ДЛЯ СИНХРОНИЗАЦИИ ВРЕМЕНИ ---
    @GetMapping("/time")
    public Map<String, Object> getServerTime() {
        ZonedDateTime now = ZonedDateTime.now();
        return Map.of(
            "iso", now.toString(),
            "timestamp", now.toInstant().toEpochMilli(),
            "offset", now.getOffset().toString()
        );
    }
}
