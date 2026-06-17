package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.StaffShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffShiftRepository extends JpaRepository<StaffShift, String> {
    List<StaffShift> findByTenantIdAndDate(String tenantId, LocalDate date);
    
    // Поиск конкретной смены мастера в филиале
    Optional<StaffShift> findByStaffIdAndDateAndBranchId(String staffId, LocalDate date, String branchId);
    
    // Получение ВСЕХ смен мастера за день (для проверки пересечений между филиалами)
    List<StaffShift> findByStaffIdAndDate(String staffId, LocalDate date);

    List<StaffShift> findByStaffIdAndDateBetween(String staffId, LocalDate startDate, LocalDate endDate);
}
