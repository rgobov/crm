package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    
    List<Branch> findByTenantId(String tenantId);
    
    List<Branch> findByTenantIdAndActive(String tenantId, boolean active);
}
