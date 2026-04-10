package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMemberScheduleDto {
    private String id;
    private String name;
    private String specialty;
    private String phone;
    private String photoUrl;
    private String photoData; // Always null for schedule API to prevent chunked encoding errors
    private boolean active;
    private String role;
    private String email;
    private List<String> branchIds; 

    // Schedule fields
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
    private boolean isDayOff;
}
