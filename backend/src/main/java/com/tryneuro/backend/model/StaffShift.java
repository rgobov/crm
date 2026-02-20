package com.tryneuro.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "staff_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffShift {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    // НОВОЕ: Привязка смены к конкретному филиалу
    @Column(name = "branch_id")
    private String branchId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    @Column(nullable = false)
    private boolean isDayOff = false;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
