package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    // --- Методы, которые были нужны AppointmentService ---
    List<Appointment> findByTenantIdAndDate(String tenantId, LocalDate date);
    List<Appointment> findByTenantIdAndStaffMemberIdAndDate(String tenantId, String staffMemberId, LocalDate date);

    // --- Методы, которые были нужны ScheduleService ---
    List<Appointment> findByDateAndTenantId(LocalDate date, String tenantId);
    List<Appointment> findByStaffMemberIdAndDate(String staffMemberId, LocalDate date);
    List<Appointment> findByResourceIdAndDate(String resourceId, LocalDate date);
    
    // --- Общий метод ---
    List<Appointment> findByTenantId(String tenantId);
}
