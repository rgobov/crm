package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiContactCreateRequest {
    private String tenantId;
    private String name;
    private String phone;
    private String email;
    private String notes;
}