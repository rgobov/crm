package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyRequest {
    private String companyName;
    private String companyAddress;
    private String adminEmail;
    private String adminPassword;
}
