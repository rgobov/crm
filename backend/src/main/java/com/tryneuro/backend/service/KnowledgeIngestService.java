package com.tryneuro.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public void ingest(String tenantId, String knowledgeId, String text) {
        List<String> chunks = chunkText(text, 512, 50);
        log.info("Ingesting {} chunks for knowledge {} / tenant {}", chunks.size(), knowledgeId, tenantId);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Double> embedding = embeddingService.embed(chunk);

            jdbcTemplate.update(
                "INSERT INTO ai_knowledge_chunks (id, tenant_id, knowledge_id, chunk_index, content, embedding) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?::vector)",
                UUID.randomUUID().toString(),
                tenantId,
                knowledgeId,
                i,
                chunk,
                embedding.toString()
            );
        }
    }

    public void reindex(String tenantId) {
        jdbcTemplate.update("DELETE FROM ai_knowledge_chunks WHERE tenant_id = ?::uuid", tenantId);
        log.info("Cleared chunks for tenant {}", tenantId);

        List<String> knowledgeIds = jdbcTemplate.queryForList(
            "SELECT id FROM ai_knowledge WHERE tenant_id = ?::uuid", String.class, tenantId
        );

        for (String kid : knowledgeIds) {
            String text = jdbcTemplate.queryForObject(
                "SELECT question || ' ' || answer FROM ai_knowledge WHERE id = ?::uuid",
                String.class, kid
            );
            if (text != null && !text.isEmpty()) {
                ingest(tenantId, kid, text);
            }
        }
    }

    private List<String> chunkText(String text, int maxTokens, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        int start = 0;

        while (start < words.length) {
            int end = Math.min(start + maxTokens, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) chunk.append(" ");
                chunk.append(words[i]);
            }
            chunks.add(chunk.toString());
            if (end == words.length) break;
            start = end - overlap;
        }

        return chunks;
    }
}
