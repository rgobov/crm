package com.tryneuro.backend.repository;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    @Query("SELECT a FROM Appointment a WHERE CAST(a.startTime AS date) = :date AND a.tenantId = :tenantId")
    List<Appointment> findByDateAndTenantId(@Param("date") LocalDate date, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.resourceId = :resourceId AND CAST(a.startTime AS date) = :date")
    List<Appointment> findByResourceIdAndDate(@Param("resourceId") String resourceId, @Param("date") LocalDate date);

    List<Appointment> findByTenantId(String tenantId);

    boolean existsByStaffMemberId(String staffId);

    // НОВЫЙ МЕТОД ДЛЯ УНИВЕРСАЛЬНОГО ПЛАНИРОВЩИКА
    List<Appointment> findAllByReminderSentFalseAndAllowReminderTrueAndStartTimeAfter(OffsetDateTime time);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId " +
           "AND (a.reminderSent IS NULL OR a.reminderSent = false) " +
           "AND CAST(a.startTime AS date) >= :today " +
           "AND a.contactId IS NOT NULL")
    List<Appointment> findPendingReminders(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND a.staffMemberId = :staffId AND CAST(a.startTime AS date) = :date")
    List<Appointment> findByTenantIdAndStaffMemberIdAndDate(@Param("tenantId") String tenantId, @Param("staffId") String staffId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.contactId = :contactId AND a.tenantId = :tenantId ORDER BY a.startTime DESC")
    List<Appointment> findByContactIdAndTenantIdOrderByDateDesc(@Param("contactId") String contactId, @Param("tenantId") String tenantId);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.startTime), COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month " +
           "GROUP BY DAY(a.startTime)")
    List<WorkloadDto> getWorkloadForMonth(@Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.startTime), COUNT(a)) " +
           "FROM Appointment a WHERE a.staffMemberId = :staffId AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month " +
           "GROUP BY DAY(a.startTime)")
    List<WorkloadDto> getWorkloadForStaffAndMonth(@Param("staffId") String staffId, @Param("year") int year, @Param("month") int month);
}
