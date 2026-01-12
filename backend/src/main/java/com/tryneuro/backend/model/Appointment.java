package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "contact_id")
    private String contactId;

    @Column(nullable = false)
    private String service;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "staff_member_id")
    private String staffMemberId;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private String comment;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // --- НОВОЕ ПОЛЕ: Мировое время создания записи ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
