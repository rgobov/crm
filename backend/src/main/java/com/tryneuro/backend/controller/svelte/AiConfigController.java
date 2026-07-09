package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserAiConfig;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.service.UserAiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiConfigController {

    private final UserAiConfigService userAiConfigService;
    private final UserRepository userRepository;

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
        String llmProvider = (String) config.getOrDefault("llm_provider", "gigachat");
        String llmModel = (String) config.getOrDefault("llm_model", "GigaChat");
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
}
