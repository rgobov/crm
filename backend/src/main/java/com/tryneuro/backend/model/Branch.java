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



@Column(nullable = false, columnDefinition = "boolean default true")

    private boolean active = true;



    @Column(nullable = false, columnDefinition = "varchar(20) default 'AUTO'")

    private String niche = "AUTO";



}
