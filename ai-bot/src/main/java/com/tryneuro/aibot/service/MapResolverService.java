package com.tryneuro.aibot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MapResolverService {

    private static final Logger log = LoggerFactory.getLogger(MapResolverService.class);

    private final RestTemplate rest;
    private final ObjectMapper mapper;
    private final String backendUrl;
    private final String internalSecret;

    public MapResolverService(RestTemplate rest, ObjectMapper mapper) {
        this.rest = rest;
        this.mapper = mapper;
        String url = System.getenv("CRM_BACKEND_URL");
        this.backendUrl = (url != null && !url.isEmpty()) ? url : "http://backend:8080";
        String secret = System.getenv("INTERNAL_SECRET");
        this.internalSecret = (secret != null && !secret.isEmpty()) ? secret : "try-neuro-internal-secret-2026";
    }

    public Map<String, String> resolveActor(long chatId) {
        log.info("resolveActor: chat_id={}", chatId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            long startMs = System.currentTimeMillis();
            String json = rest.getForObject(
                backendUrl + "/api/admin/ai/internal/users/by-telegram/" + chatId,
                String.class);
            long elapsed = System.currentTimeMillis() - startMs;

            log.info("resolveActor: backend responded in {}ms, json={}", elapsed, json);
            Map<String, Object> data = mapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});

            Map<String, String> result = Map.of(
                "role", String.valueOf(data.getOrDefault("role", "CLIENT")),
                "contact_id", String.valueOf(data.getOrDefault("contactId", "")),
                "staff_id", String.valueOf(data.getOrDefault("staffId", "")),
                "tenant_id", String.valueOf(data.getOrDefault("tenantId", "")),
                "user_id", String.valueOf(data.getOrDefault("userId", ""))
            );
            log.info("resolveActor result: role={}, tenantId={}, contactId={}, staffId={}",
                result.get("role"), result.get("tenant_id"),
                result.get("contact_id"), result.get("staff_id"));
            return result;
        } catch (Exception e) {
            log.warn("resolveActor error for chat_id {}: {}", chatId, e.getMessage(), e);
            return Map.of(
                "role", "CLIENT",
                "contact_id", "",
                "staff_id", "",
                "tenant_id", "",
                "user_id", ""
            );
        }
    }
}
