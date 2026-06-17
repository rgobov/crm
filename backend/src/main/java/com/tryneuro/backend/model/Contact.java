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

    @Column(name = "phones", nullable = false)

    private List<String> phones = new ArrayList<>();



    private String email;



    @Column(columnDefinition = "TEXT")

    private String notes;



    @Column(name = "tenant_id", nullable = false)

    private String tenantId;



    // НОВОЕ: Массив тегов/автомобилей для поиска и быстрой привязки

    @JdbcTypeCode(SqlTypes.ARRAY)

    @Column(name = "tags")

    private List<String> tags = new ArrayList<>();



    // Геттеры и сеттеры для обхода проблем с Lombok

    public String getId() { return id; }

    public String getName() { return name; }

    public List<String> getPhones() { return phones; }

    public String getEmail() { return email; }

    public String getNotes() { return notes; }

    public String getTenantId() { return tenantId; }

    public List<String> getTags() { return tags; }



    public void setId(String id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setPhones(List<String> phones) { this.phones = phones; }

    public void setEmail(String email) { this.email = email; }

    public void setNotes(String notes) { this.notes = notes; }

    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public void setTags(List<String> tags) { this.tags = tags; }

}

