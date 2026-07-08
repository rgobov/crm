package com.tryneuro.backend.dto.ai;

import lombok.Data;

@Data
public class AiRagSearchRequest {
    private String tenantId;
    private String query;
    private int topK = 5;
}
