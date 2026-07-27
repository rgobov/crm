package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDto {
    private String id;
    private String name;
    private String description;
    private String branchId;
    private Long photoUpdatedAt;
}
