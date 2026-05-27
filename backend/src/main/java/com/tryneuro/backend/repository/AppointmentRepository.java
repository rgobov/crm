package com.tryneuro.backend.repository;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    @Query("SELECT a FROM Appointment a WHERE CAST(a.startTime AS date) = :date AND a.tenantId = :tenantId")
    List<Appointment> findByDateAndTenantId(@Param("date") LocalDate date, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM Appointment a WHERE CAST(a.startTime AS date) = :date AND a.tenantId = :tenantId " +
           "AND (:branchId IS NULL OR :branchId = '' OR a.branchId = :branchId)")
    List<Appointment> findByDateAndTenantIdAndBranchId(@Param("date") LocalDate date, 
                                                       @Param("tenantId") String tenantId, 
                                                       @Param("branchId") String branchId);

    @Modifying
    @Query("UPDATE Appointment a SET a.clientName = :newName WHERE a.contactId = :contactId AND a.tenantId = :tenantId")
    void updateClientNameForContact(@Param("contactId") String contactId, @Param("newName") String newName, @Param("tenantId") String tenantId);

    // НОВОЕ: Поиск записей конкретного мастера в конкретном филиале (для проверки выходных)
    @Query("SELECT a FROM Appointment a WHERE a.staffMemberId = :staffId AND a.branchId = :branchId " +
           "AND CAST(a.startTime AS date) = :date AND a.status != 'CANCELLED'")
    List<Appointment> findByStaffIdAndBranchIdAndDate(@Param("staffId") String staffId, 
                                                      @Param("branchId") String branchId, 
                                                      @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.resourceId = :resourceId AND CAST(a.startTime AS date) = :date")
    List<Appointment> findByResourceIdAndDate(@Param("resourceId") String resourceId, @Param("date") LocalDate date);

    List<Appointment> findByTenantId(String tenantId);

    boolean existsByStaffMemberId(String staffId);

    @Query("SELECT MAX(COALESCE(a.reminderLeadTimeHours, 24)) FROM Appointment a WHERE a.reminderSent = false AND a.allowReminder = true")
    Integer findMaxReminderLeadTimeHours();

    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.contact " +
           "WHERE a.reminderSent = false AND a.allowReminder = true " +
           "AND a.startTime > :now AND a.startTime <= :maxStartTime")
    List<Appointment> findAppointmentsForReminders(@Param("now") OffsetDateTime now, 
                                                   @Param("maxStartTime") OffsetDateTime maxStartTime);

    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.reminderSent = :sent WHERE a.id = :id")
    void updateReminderSentStatus(@Param("id") String id, @Param("sent") boolean sent);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND a.staffMemberId = :staffId AND CAST(a.startTime AS date) = :date")
    List<Appointment> findByTenantIdAndStaffMemberIdAndDate(@Param("tenantId") String tenantId, @Param("staffId") String staffId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.contactId = :contactId AND a.tenantId = :tenantId ORDER BY a.startTime DESC")
    List<Appointment> findByContactIdAndTenantIdOrderByDateDesc(@Param("contactId") String contactId, @Param("tenantId") String tenantId);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.startTime), COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month " +
           "GROUP BY DAY(a.startTime)")
    List<WorkloadDto> getWorkloadForMonth(@Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.startTime), COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId " +
           "AND (:branchId IS NULL OR :branchId = '' OR a.branchId = :branchId) " +
           "AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month " +
           "GROUP BY DAY(a.startTime)")
    List<WorkloadDto> getWorkloadForMonthAndBranch(@Param("tenantId") String tenantId, 
                                                   @Param("year") int year, 
                                                   @Param("month") int month, 
                                                   @Param("branchId") String branchId);

    @Query("SELECT new com.tryneuro.backend.dto.WorkloadDto(DAY(a.startTime), COUNT(a)) " +
           "FROM Appointment a WHERE a.staffMemberId = :staffId AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month " +
           "GROUP BY DAY(a.startTime)")
    List<WorkloadDto> getWorkloadForStaffAndMonth(@Param("staffId") String staffId, @Param("year") int year, @Param("month") int month);
}
