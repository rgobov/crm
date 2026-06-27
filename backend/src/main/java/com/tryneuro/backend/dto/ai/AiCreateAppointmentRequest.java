package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCreateAppointmentRequest {
    private String tenantId;
    private String clientName;
    private String clientPhone;
    private String contactId;
    private String serviceName;
    private String staffName;
    private String branchId;
    private String dateTime;
    private Integer durationMinutes;
    private String resourceId;
}