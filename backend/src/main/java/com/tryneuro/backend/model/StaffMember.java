package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
