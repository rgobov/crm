package com.tryneuro.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "staff_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "photo_data")
    private byte[] photoData;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "staff_member_branches",
        joinColumns = @JoinColumn(name = "staff_member_id"),
        inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private Set<Branch> branches;

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
