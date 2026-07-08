package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AiRagSearchResponse {
    private List<ChunkResult> chunks;

    @Data
    @AllArgsConstructor
    public static class ChunkResult {
        private String content;
        private double score;
        private Map<String, Object> metadata;
    }
}
