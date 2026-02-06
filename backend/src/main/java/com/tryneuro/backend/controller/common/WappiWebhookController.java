package com.tryneuro.backend.controller.common;

import com.tryneuro.backend.service.WappiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/wappi")
public class WappiWebhookController {

    private final WappiService wappiService;

    @Autowired
    public WappiWebhookController(WappiService wappiService) {
        this.wappiService = wappiService;
    }

    @PostMapping
    public void handleWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received Wappi Webhook: " + payload);
    }
}
