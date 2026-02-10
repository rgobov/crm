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

    private String phone;

    // Ссылка на аватар (опционально)
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Transient
    private LocalTime workStartTime;
    @Transient
    private LocalTime workEndTime;
    @Transient
    private LocalTime breakStartTime;
    @Transient
    private LocalTime breakEndTime;
    @Transient
    private boolean isDayOff;

    @Transient
    private String role;

    @Transient
    private String email;
}
