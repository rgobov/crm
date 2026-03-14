package com.tryneuro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {
    private String id;
    private String name;
    private List<String> phones;
    private String email;
    private String notes;
    private List<String> tags;
}
