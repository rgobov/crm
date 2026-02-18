package com.tryneuro.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", insertable = false, updatable = false)
    private Contact contact;

    @Column(name = "contact_id")
    private String contactId;

    @Column(nullable = false)
    private String service;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", insertable = false, updatable = false)
    private Resource resource;

    @Column(name = "resource_id")
    private String resourceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_member_id", insertable = false, updatable = false)
    private StaffMember staffMember;

    @Column(name = "staff_member_id")
    private String staffMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @Column(name = "branch_id")
    private String branchId;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    private String comment;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;

    @Column(name = "allow_reminder", nullable = false)
    private boolean allowReminder = true;

    @Column(name = "reminder_lead_time_hours")
    private Integer reminderLeadTimeHours = 24; // Значение по умолчанию 24 часа

    public LocalDate getDate() {
        return startTime != null ? startTime.toLocalDate() : null;
    }

    public LocalTime getTime() {
        return startTime != null ? startTime.toLocalTime() : null;
    }
}
