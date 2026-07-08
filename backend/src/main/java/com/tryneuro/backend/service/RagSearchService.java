package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.ai.AiRagSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public AiRagSearchResponse search(String tenantId, String query, int topK) {
        List<Double> queryEmbedding = embeddingService.embed(query);

        if (queryEmbedding.stream().allMatch(d -> d == 0.0)) {
            log.warn("Zero embedding returned for query '{}', returning empty", query);
            return new AiRagSearchResponse(List.of());
        }

        String vectorStr = queryEmbedding.toString();
        String sql = """
            SELECT content, 1 - (embedding <=> ?::vector) AS score, metadata
            FROM ai_knowledge_chunks
            WHERE tenant_id = ?::uuid AND embedding IS NOT NULL
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;

        List<AiRagSearchResponse.ChunkResult> results = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorStr, tenantId, vectorStr, topK);
            for (Map<String, Object> row : rows) {
                String content = (String) row.get("content");
                double score = ((Number) row.get("score")).doubleValue();
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) row.get("metadata");
                results.add(new AiRagSearchResponse.ChunkResult(content, score, metadata));
            }
        } catch (Exception e) {
            log.error("RAG search failed for tenant {}: {}", tenantId, e.getMessage());
        }

        return new AiRagSearchResponse(results);
    }
}
