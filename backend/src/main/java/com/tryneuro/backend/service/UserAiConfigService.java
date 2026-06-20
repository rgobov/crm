package com.tryneuro.backend.service;

import com.tryneuro.backend.model.UserAiConfig;
import com.tryneuro.backend.repository.UserAiConfigRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAiConfigService {

    private final UserAiConfigRepository repository;

    public UserAiConfig getConfig(String userId) {
        return repository.findById(userId)
                .orElseGet(() -> defaultConfig(userId));
    }

    @Transactional
    public UserAiConfig saveConfig(String userId, String llmProvider, String llmModel, String apiKey, String sttProvider) {
        UserAiConfig config = repository.findById(userId).orElseGet(() -> defaultConfig(userId));
        config.setLlmProvider(llmProvider);
        config.setLlmModel(llmModel);
        config.setApiKey(apiKey);
        config.setSttProvider(sttProvider);
        config.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(config);
    }

    private UserAiConfig defaultConfig(String userId) {
        UserAiConfig config = new UserAiConfig();
        config.setUserId(userId);
        return config;
    }
}