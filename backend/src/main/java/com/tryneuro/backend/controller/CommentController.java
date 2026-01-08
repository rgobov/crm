package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CommentRequest;
import com.tryneuro.backend.model.AppointmentComment;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.service.CommentService;
import jakarta.validation.Valid; // <<< ИМПОРТ
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/appointment/{appointmentId}")
    public List<AppointmentComment> getComments(@RequestAttribute("tenantId") String tenantId, 
                                                @PathVariable String appointmentId) {
        return commentService.getCommentsForAppointment(tenantId, appointmentId);
    }

    // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Добавляем @Valid ---
    @PostMapping("/appointment/{appointmentId}")
    public AppointmentComment addComment(@RequestAttribute("tenantId") String tenantId,
                                         @PathVariable String appointmentId,
                                         @Valid @RequestBody CommentRequest request, // <<< АННОТАЦИЯ
                                         @AuthenticationPrincipal User user) {
        return commentService.addCommentToAppointment(tenantId, appointmentId, request.getText(), user);
    }
}
