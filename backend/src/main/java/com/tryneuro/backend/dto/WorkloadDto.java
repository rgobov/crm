package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadDto {
    private Integer day;
    private Long appointmentCount;
}
