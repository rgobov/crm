package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterClientRequest {
    private String email;
    private String password;
    private String name;
    private String phone;
    private String tenantId;
    private Boolean agreedToPolicy;
}
