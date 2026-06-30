package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDto {
    private String id;
    private String consentType;
    private String policyVersion;
    private String ipAddress;
    private LocalDateTime acceptedAt;
    private LocalDateTime revokedAt;
}
