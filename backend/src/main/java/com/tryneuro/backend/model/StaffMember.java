package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "staff_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String specialty;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // График работы
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    // Доступность (например, для отпуска или больничного)
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean available = true;

    @Transient
    private String role;
}
