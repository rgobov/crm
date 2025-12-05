package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, String> {
    List<StaffMember> findByTenantId(String tenantId);
}
