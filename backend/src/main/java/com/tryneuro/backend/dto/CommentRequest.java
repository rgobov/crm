package com.tryneuro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(max = 500, message = "Комментарий не может превышать 500 символов")
    private String text;
}
