package com.tryneuro.backend.dto;

import lombok.Data;

@Data
public class CreateStaffRequest {
    // Данные сотрудника
    private String name;
    private String specialty;
    private String phone; // Добавлено поле телефона
    private boolean available;
    private String workStartTime;
    private String workEndTime;
    private String breakStartTime;
    private String breakEndTime;

    // Данные для входа
    private String email;
    private String password;
    private String role;
}
