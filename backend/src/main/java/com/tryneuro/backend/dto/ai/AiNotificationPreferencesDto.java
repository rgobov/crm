package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiNotificationPreferencesDto {
    private boolean notificationEnabled;
    private int notificationLeadTimeHours;
}
