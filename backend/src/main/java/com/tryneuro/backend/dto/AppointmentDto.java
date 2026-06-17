package com.tryneuro.backend.dto;

import com.tryneuro.backend.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private String id;
    private OffsetDateTime startTime;
    private Integer durationInMinutes;
    private String clientName;
    private String clientPhone;
    private String contactId;
    private String service;
    private String resourceId;
    private String staffMemberId;
    private String branchId; // Исправлено на camelCase
    private AppointmentStatus status;
    private String comment;
    private String referenceTag;
    private boolean reminderSent;
    private boolean allowReminder;
    private Integer reminderLeadTimeHours;
    private String groupId;
    private java.util.List<String> staffMemberIds;
}
