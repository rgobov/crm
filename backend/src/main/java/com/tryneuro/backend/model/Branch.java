package com.tryneuro.backend.model;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;



@Entity

@Table(name = "branches")

@Data

@NoArgsConstructor

@AllArgsConstructor

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Branch {

    @Id

    @GeneratedValue(strategy = GenerationType.UUID)

    private String id;



    @Column(nullable = false)

    private String name;



    private String address;



    @Column(nullable = false)

    private String timezone;



    @Column(name = "tenant_id", nullable = false)

    private String tenantId;



    // Геттеры для обхода проблем с Lombok

    public String getId() { return id; }

    public String getName() { return name; }

    public String getAddress() { return address; }

    public String getTimezone() { return timezone; }

    public String getTenantId() { return tenantId; }

}

