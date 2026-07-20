package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.ReturnReminderCandidate;
import com.tryneuro.backend.dto.SendReturnReminderRequest;
import com.tryneuro.backend.service.ReturnReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/return-reminders")
@RequiredArgsConstructor
public class ReturnReminderController {

    private final ReturnReminderService returnReminderService;

    @GetMapping
    public List<ReturnReminderCandidate> getCandidates(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(defaultValue = "30") int days) {
        return returnReminderService.getCandidates(tenantId, days);
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendReminder(
            @RequestAttribute("tenantId") String tenantId,
            @RequestBody SendReturnReminderRequest request) {
        Map<String, Object> result = returnReminderService.sendReminder(
                request.getContactId(), request.getMessage(), tenantId);
        if ((boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}
