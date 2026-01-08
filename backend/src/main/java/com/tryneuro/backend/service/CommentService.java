package com.tryneuro.backend.service;

import com.tryneuro.backend.model.AppointmentComment;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.AppointmentCommentRepository;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private final AppointmentCommentRepository commentRepository;
    private final AppointmentRepository appointmentRepository;
    private final StaffMemberRepository staffMemberRepository; // <<< ДОБАВЛЯЕМ ЗАВИСИМОСТЬ

    @Autowired
    public CommentService(AppointmentCommentRepository commentRepository, 
                          AppointmentRepository appointmentRepository, 
                          StaffMemberRepository staffMemberRepository) { // <<< ДОБАВЛЯЕМ В КОНСТРУКТОР
        this.commentRepository = commentRepository;
        this.appointmentRepository = appointmentRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    public List<AppointmentComment> getCommentsForAppointment(String tenantId, String appointmentId) {
        appointmentRepository.findById(appointmentId)
                .filter(app -> app.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        return commentRepository.findByTenantIdAndAppointmentIdOrderByCreatedAtAsc(tenantId, appointmentId);
    }

    public AppointmentComment addCommentToAppointment(String tenantId, String appointmentId, String text, User author) {
        appointmentRepository.findById(appointmentId)
                .filter(app -> app.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));

        // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Получаем имя сотрудника ---
        String authorName = staffMemberRepository.findById(author.getStaffId())
                .map(StaffMember::getName) // Получаем имя, если сотрудник найден
                .orElse(author.getEmail()); // Если нет - используем email как запасной вариант

        AppointmentComment newComment = new AppointmentComment();
        newComment.setTenantId(tenantId);
        newComment.setAppointmentId(appointmentId);
        newComment.setText(text);
        newComment.setAuthorId(author.getStaffId());
        newComment.setAuthorName(authorName); // <<< Используем правильное имя

        return commentRepository.save(newComment);
    }
}
