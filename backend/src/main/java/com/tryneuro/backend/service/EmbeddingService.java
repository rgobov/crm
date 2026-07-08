package com.tryneuro.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    private final RestTemplate rest;
    private final String apiKey;

    public EmbeddingService(
            @Value("${openrouter.api.key:}") String apiKey,
            @Value("${internal.api.secret:try-neuro-internal-secret-2026}") String internalSecret) {
        this.rest = new RestTemplate();
        this.apiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : internalSecret;
    }

    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OPENROUTER_API_KEY not set, returning zero vector");
            return java.util.Collections.nCopies(1536, 0.0);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = Map.of(
            "model", "text-embedding-ada-002",
            "input", text
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = rest.postForObject(
                "https://openrouter.ai/api/v1/embeddings",
                entity,
                Map.class
            );

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }
        } catch (Exception e) {
            log.error("Embedding API call failed: {}", e.getMessage());
        }

        return java.util.Collections.nCopies(1536, 0.0);
    }
}
