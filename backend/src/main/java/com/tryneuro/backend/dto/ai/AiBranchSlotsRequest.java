package com.tryneuro.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiBranchSlotsRequest {
    private String tenantId;
    private String branchId;
    private String branchName;
    private String date;
    private Integer duration;
}