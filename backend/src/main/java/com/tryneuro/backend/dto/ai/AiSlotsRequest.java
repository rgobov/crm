package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSlotsRequest {
    private String tenantId;
    private String staffId;
    private String date;
    private Integer duration;
}