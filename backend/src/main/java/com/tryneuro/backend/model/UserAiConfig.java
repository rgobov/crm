package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ai_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAiConfig {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "llm_provider", nullable = false)
    private String llmProvider = "gigachat";

    @Column(name = "llm_model", nullable = false)
    private String llmModel = "GigaChat";

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "stt_provider", nullable = false)
    private String sttProvider = "vosk";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}