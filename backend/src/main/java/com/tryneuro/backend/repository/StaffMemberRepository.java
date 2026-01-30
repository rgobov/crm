package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.StaffMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, String> {
    List<StaffMember> findByTenantId(String tenantId);

    @Query("SELECT s FROM StaffMember s WHERE s.tenantId = :tenantId AND s.active = true " +
           "AND (:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR s.phone LIKE CONCAT('%', :query, '%') " +
           "OR LOWER(s.specialty) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<StaffMember> findByTenantIdAndQuery(@Param("tenantId") String tenantId,
                                            @Param("query") String query,
                                            Pageable pageable);
}
