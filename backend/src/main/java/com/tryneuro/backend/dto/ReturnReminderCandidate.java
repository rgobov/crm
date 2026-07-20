package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnReminderCandidate {
    private String contactId;
    private String name;
    private String phone;
    private String lastService;
    private LocalDateTime lastVisit;
    private long daysSinceLastVisit;
}
