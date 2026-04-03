package com.tryneuro.backend.controller.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public String hello() {
        return "Try Neuro Backend is running!";
    }

    @GetMapping("/api/system/time")
    public Map<String, Object> getServerTime() {
        Map<String, Object> response = new HashMap<>();
        response.put("serverTime", LocalDateTime.now().toString());
        return response;
    }
}
