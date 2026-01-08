package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.AppointmentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentCommentRepository extends JpaRepository<AppointmentComment, String> {

    // Находим все комментарии для определенной записи и компании, 
    // отсортированные по дате создания, чтобы получился чат.
    List<AppointmentComment> findByTenantIdAndAppointmentIdOrderByCreatedAtAsc(String tenantId, String appointmentId);

}
