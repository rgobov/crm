package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.model.AiKnowledge;
import com.tryneuro.backend.service.AiKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final AiKnowledgeService aiKnowledgeService;

    @GetMapping("/knowledge")
    public ResponseEntity<?> getKnowledge(
            @RequestParam(required = false) String category,
            @RequestAttribute("tenantId") String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        return ResponseEntity.ok(aiKnowledgeService.getAll(tenantId, category));
    }

    @PostMapping("/knowledge")
    public ResponseEntity<?> addKnowledge(
            @RequestBody Map<String, String> body,
            @RequestAttribute("tenantId") String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        String question = body.get("question");
        String answer = body.get("answer");
        String category = body.get("category");

        if (question == null || question.isEmpty()) {
            return ResponseEntity.badRequest().body("question is required");
        }
        if (answer == null || answer.isEmpty()) {
            return ResponseEntity.badRequest().body("answer is required");
        }

        AiKnowledge saved = aiKnowledgeService.create(tenantId, question, answer, category);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/knowledge/{id}")
    public ResponseEntity<?> deleteKnowledge(
            @PathVariable String id,
            @RequestAttribute("tenantId") String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ID компании не найден");
        }
        try {
            aiKnowledgeService.delete(id, tenantId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
