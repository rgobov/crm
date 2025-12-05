package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    List<Resource> findByTenantId(String tenantId);
}
