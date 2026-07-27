package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {
    List<Service> findByTenantId(String tenantId);
    List<Service> findByTenantIdAndNiche(String tenantId, String niche);
    List<Service> findByTenantIdAndNicheIsNull(String tenantId);
}