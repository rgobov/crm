package com.tryneuro.backend.repository;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    List<Appointment> findByDateAndTenantId(LocalDate date, String tenantId);
    List<Appointment> findByResourceIdAndDate(String resourceId, LocalDate date); // Этот метод тоже нужно будет исправить, но позже
    List<Appointment> findByTenantId(String tenantId);

    // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Метод теперь требует tenantId ---
    List<Appointment> findByTenantIdAndStaffMemberIdAndDate(String tenantId, String staffMemberId, LocalDate date);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.date), COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND YEAR(a.date) = :year AND MONTH(a.date) = :month " +
           "GROUP BY DAY(a.date)")
    List<WorkloadDto> getWorkloadForMonth(@Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.date), COUNT(a)) " +
           "FROM Appointment a WHERE a.staffMemberId = :staffId AND YEAR(a.date) = :year AND MONTH(a.date) = :month " +
           "GROUP BY DAY(a.date)")
    List<WorkloadDto> getWorkloadForStaffAndMonth(@Param("staffId") String staffId, @Param("year") int year, @Param("month") int month);

}
