package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "phones", nullable = false, columnDefinition = "text[]")
    private List<String> phones = new ArrayList<>();

    private String email;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // НОВОЕ: Массив тегов/автомобилей для поиска и быстрой привязки
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();
}
