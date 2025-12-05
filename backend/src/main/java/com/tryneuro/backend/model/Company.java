package com.tryneuro.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Это и будет tenant_id

    @Column(nullable = false, unique = true)
    private String name;

    private String address;
    
    @Column(nullable = false)
    private String ownerEmail; // Email создателя/владельца
}
