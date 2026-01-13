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
    List<Appointment> findByResourceIdAndDate(String resourceId, LocalDate date);
    List<Appointment> findByTenantId(String tenantId);

    // --- ОПТИМИЗИРОВАННЫЙ МЕТОД ДЛЯ ПЛАНИРОВЩИКА ---
    // Ищет записи:
    // 1. Для конкретной компании (tenantId)
    // 2. У которых еще НЕ отправлено напоминание (reminderSent = false)
    // 3. Дата которых сегодня или позже (date >= :today)
    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId " +
           "AND (a.reminderSent IS NULL OR a.reminderSent = false) " +
           "AND a.date >= :today " +
           "AND a.contactId IS NOT NULL")
    List<Appointment> findPendingReminders(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    List<Appointment> findByTenantIdAndStaffMemberIdAndDate(String tenantId, String staffMemberId, LocalDate date);

    List<Appointment> findByContactIdAndTenantIdOrderByDateDesc(String contactId, String tenantId);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.date), COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND YEAR(a.date) = :year AND MONTH(a.date) = :month " +
           "GROUP BY DAY(a.date)")
    List<WorkloadDto> getWorkloadForMonth(@Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.date), COUNT(a)) " +
           "FROM Appointment a WHERE a.staffMemberId = :staffId AND YEAR(a.date) = :year AND MONTH(a.date) = :month " +
           "GROUP BY DAY(a.date)")
    List<WorkloadDto> getWorkloadForStaffAndMonth(@Param("staffId") String staffId, @Param("year") int year, @Param("month") int month);

}
