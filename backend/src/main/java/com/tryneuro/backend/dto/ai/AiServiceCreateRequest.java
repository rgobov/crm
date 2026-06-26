package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceCreateRequest {
    private String tenantId;
    private String name;
    private Integer durationMinutes;
    private Integer priceMin;
    private Integer priceMax;
}
