package com.tryneuro.backend.controller.common;

import com.tryneuro.backend.model.NotificationTemplate;
import com.tryneuro.backend.service.NotificationTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;
    private final HttpServletRequest request;

    @GetMapping
    public ResponseEntity<List<NotificationTemplate>> getTemplates() {
        String tenantId = (String) request.getAttribute("tenantId");
        return ResponseEntity.ok(templateService.getAllTemplates(tenantId));
    }

    @PostMapping
    public ResponseEntity<NotificationTemplate> saveTemplate(@RequestBody NotificationTemplate template) {
        String tenantId = (String) request.getAttribute("tenantId");
        return ResponseEntity.ok(templateService.saveTemplate(tenantId, template));
    }
}
