package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserAiConfig;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.service.UserAiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiConfigController {

    private final UserAiConfigService userAiConfigService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.knowledge.service.url:http://ai-knowledge-service:8082}")
    private String aiKnowledgeUrl;

    private String getRequiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID is required");
        }
        return tenantId;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", "try-neuro-internal-secret-2026");
        return headers;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        UserAiConfig config = userAiConfigService.getConfig(user.getId());
        return ResponseEntity.ok(Map.of(
                "llm_provider", config.getLlmProvider(),
                "llm_model", config.getLlmModel(),
                "api_key", config.getApiKey(),
                "stt_provider", config.getSttProvider(),
                "telegram_id", user.getTelegramId()
        ));
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(
            @RequestBody Map<String, Object> config,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String llmProvider = (String) config.getOrDefault("llm_provider", "openrouter");
        String llmModel = (String) config.getOrDefault("llm_model", "openrouter/auto");
        String apiKey = (String) config.getOrDefault("api_key", "");
        String sttProvider = (String) config.getOrDefault("stt_provider", "vosk");
        
        Object telegramIdRaw = config.get("telegram_id");
        if (telegramIdRaw != null) {
            Long telegramId = telegramIdRaw instanceof Number
                    ? ((Number) telegramIdRaw).longValue()
                    : Long.valueOf(telegramIdRaw.toString());
            user.setTelegramId(telegramId);
            userRepository.save(user);
        }

        UserAiConfig saved = userAiConfigService.saveConfig(user.getId(), llmProvider, llmModel, apiKey, sttProvider);

        return ResponseEntity.ok(Map.of(
                "llm_provider", saved.getLlmProvider(),
                "llm_model", saved.getLlmModel(),
                "api_key", saved.getApiKey(),
                "stt_provider", saved.getSttProvider()
        ));
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