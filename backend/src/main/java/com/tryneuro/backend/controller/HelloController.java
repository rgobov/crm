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
        System.out.println("RECEIVED REQUEST: /api/system/hello at " + ZonedDateTime.now());
        return "Hello from TryNeuro Backend!";
    }

    @GetMapping("/time")
    public Map<String, Object> getServerTime() {
        return Map.of(
            "iso", ZonedDateTime.now().toString(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
