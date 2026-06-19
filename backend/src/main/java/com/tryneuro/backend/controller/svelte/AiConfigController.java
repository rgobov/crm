package com.tryneuro.backend.controller.svelte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiConfigController {

    @Value("${internal.api.secret:try-neuro-internal-secret-2026}")
    private String internalSecret;

    @Value("${ai.knowledge.service.url:http://ai-knowledge-service:8082}")
    private String aiKnowledgeUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant ID not found");
        }
        return tenantId;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Internal-Secret", internalSecret);
        return h;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@RequestAttribute("tenantId") String tenantId) {
        String tId = getRequiredTenantId(tenantId);
        var exchange = restTemplate.exchange(
                aiKnowledgeUrl + "/api/v1/config/" + tId,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Map.class
        );
        return ResponseEntity.ok(exchange.getBody());
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(
            @RequestBody Map<String, Object> config,
            @RequestAttribute("tenantId") String tenantId) {
        String tId = getRequiredTenantId(tenantId);
        var exchange = restTemplate.exchange(
                aiKnowledgeUrl + "/api/v1/config/" + tId,
                HttpMethod.PUT,
                new HttpEntity<>(config, headers()),
                Map.class
        );
        return ResponseEntity.ok(exchange.getBody());
    }

    @GetMapping("/knowledge")
    public ResponseEntity<?> getKnowledge(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String category) {
        String tId = getRequiredTenantId(tenantId);
        String url = aiKnowledgeUrl + "/api/v1/knowledge/" + tId;
        if (category != null && !category.isEmpty()) {
            url += "?category=" + category;
        }
        var exchange = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        );
        return ResponseEntity.ok(exchange.getBody());
    }

    @PostMapping("/knowledge")
    public ResponseEntity<?> addKnowledge(
            @RequestBody Map<String, Object> entry,
            @RequestAttribute("tenantId") String tenantId) {
        String tId = getRequiredTenantId(tenantId);
        var exchange = restTemplate.exchange(
                aiKnowledgeUrl + "/api/v1/knowledge/" + tId,
                HttpMethod.POST,
                new HttpEntity<>(entry, headers()),
                Map.class
        );
        return ResponseEntity.ok(exchange.getBody());
    }

    @DeleteMapping("/knowledge/{id}")
    public ResponseEntity<?> deleteKnowledge(
            @PathVariable String id,
            @RequestAttribute("tenantId") String tenantId) {
        getRequiredTenantId(tenantId);
        restTemplate.exchange(
                aiKnowledgeUrl + "/api/v1/knowledge/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(headers()),
                Void.class
        );
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}