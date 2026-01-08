package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "author_id", nullable = false)
    private String authorId; // staffId того, кто написал

    @Column(name = "author_name", nullable = false)
    private String authorName; // Имя того, кто написал

    // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Убираем @Lob и явно указываем тип TEXT ---
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
