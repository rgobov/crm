package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSettings {
    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "connected_phone")
    private String connectedPhone;

    @Column(name = "is_active")
    private boolean isActive = false;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @OneToOne
    @MapsId
    @JoinColumn(name = "tenant_id")
    private Company company;
}
