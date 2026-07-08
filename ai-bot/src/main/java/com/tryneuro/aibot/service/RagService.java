package com.tryneuro.aibot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final RestTemplate rest;
    private final ObjectMapper mapper;
    private final String backendUrl;
    private final String internalSecret;

    public RagService(RestTemplate rest, ObjectMapper mapper) {
        this.rest = rest;
        this.mapper = mapper;
        String url = System.getenv("CRM_BACKEND_URL");
        this.backendUrl = (url != null && !url.isEmpty()) ? url : "http://backend:8080";
        String secret = System.getenv("INTERNAL_SECRET");
        this.internalSecret = (secret != null && !secret.isEmpty()) ? secret : "try-neuro-internal-secret-2026";
    }

    public String enhancePrompt(String tenantId, String query) {
        if (query == null || query.trim().isEmpty()) {
            log.debug("RAG: empty query, skipping");
            return "";
        }
        log.info("RAG enhancePrompt: tenantId={}, query=\"{}\"", tenantId, query);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = Map.of(
                "tenantId", tenantId,
                "query", query,
                "topK", 3
            );

            String json = mapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            long startMs = System.currentTimeMillis();
            String response = rest.postForObject(
                backendUrl + "/api/admin/ai/internal/knowledge/rag-search",
                entity,
                String.class
            );
            long elapsed = System.currentTimeMillis() - startMs;

            if (response == null) {
                log.warn("RAG: null response from backend (elapsed={}ms)", elapsed);
                return "";
            }
            log.info("RAG: response in {}ms, raw len={}", elapsed, response.length());

            Map<String, Object> parsed = mapper.readValue(response,
                new TypeReference<Map<String, Object>>() {});
            Object chunksObj = parsed.get("chunks");

            if (chunksObj instanceof List<?> chunks) {
                log.info("RAG: got {} chunks", chunks.size());
                if (!chunks.isEmpty()) {
                    StringBuilder context = new StringBuilder("\n\nКонтекст из базы знаний:\n");
                    for (Object chunkObj : chunks) {
                        if (chunkObj instanceof Map<?, ?> chunk) {
                            String content = (String) chunk.get("content");
                            if (content != null) {
                                context.append("- ").append(content).append("\n");
                            }
                        }
                    }
                    log.info("RAG: context built, len={}", context.length());
                    return context.toString();
                }
            } else {
                log.warn("RAG: response has no 'chunks' field, keys: {}", parsed.keySet());
            }
        } catch (Exception e) {
            log.warn("RAG enhance failed for tenant {}: {}", tenantId, e.getMessage(), e);
        }

        return "";
    }
}
