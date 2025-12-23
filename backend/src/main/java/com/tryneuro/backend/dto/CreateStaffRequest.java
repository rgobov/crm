package com.tryneuro.backend.dto;

import lombok.Data;

@Data
public class CreateStaffRequest {
    // Данные сотрудника
    private String name;
    private String specialty;

    // Данные для входа (опционально, если сотрудник будет пользователем системы)
    private String email;
    private String password;
    private String role; // "MANAGER" или "EMPLOYEE"
}
