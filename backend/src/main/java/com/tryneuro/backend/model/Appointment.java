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
import java.time.ZoneId;

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

    @Column(name = "client_phone")
    private String clientPhone;

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

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    private String comment;

    @Column(name = "reference_tag")
    private String referenceTag;

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
    private Integer reminderLeadTimeHours = 24;

    // ФИНАЛЬНЫЙ ФИКС: Автоматически превращаем пустые строки в NULL перед сохранением
    @PrePersist
    @PreUpdate
    private void sanitizeIds() {
        if (resourceId != null && resourceId.trim().isEmpty()) resourceId = null;
        if (staffMemberId != null && staffMemberId.trim().isEmpty()) staffMemberId = null;
        if (contactId != null && contactId.trim().isEmpty()) contactId = null;
    }

    public LocalDate getDate() {
        if (startTime == null) return null;
        String tz = (branch != null && branch.getTimezone() != null) ? branch.getTimezone() : "Europe/Moscow";
        return startTime.atZoneSameInstant(ZoneId.of(tz)).toLocalDate();
    }

    public LocalTime getTime() {
        if (startTime == null) return null;
        String tz = (branch != null && branch.getTimezone() != null) ? branch.getTimezone() : "Europe/Moscow";
        return startTime.atZoneSameInstant(ZoneId.of(tz)).toLocalTime();
    }
}
