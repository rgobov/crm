package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiReportRequest {
    private String tenantId;
    private String reportType;
    private String period;
    private String date;
}