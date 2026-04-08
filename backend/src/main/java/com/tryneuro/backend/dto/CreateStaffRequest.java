package com.tryneuro.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateStaffRequest {
    private String name;
    private String specialty;
    private String phone;
    private String email;
    private String password;
    private String role;
    private boolean available;
    private List<String> branchIds; // ПОДДЕРЖКА НЕСКОЛЬКИХ ФИЛИАЛОВ
    private String photoData; // Base64 encoded photo
}
