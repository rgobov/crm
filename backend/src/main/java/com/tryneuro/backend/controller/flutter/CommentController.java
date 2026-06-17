package com.tryneuro.backend.controller.flutter;

import com.tryneuro.backend.dto.CommentRequest;
import com.tryneuro.backend.model.AppointmentComment;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.service.CommentService;
import jakarta.validation.Valid;
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

    @GetMapping("/appointment/{id}")
    public List<AppointmentComment> getComments(@RequestAttribute("tenantId") String tenantId, @PathVariable String id) {
        return commentService.getCommentsForAppointment(tenantId, id);
    }

    @PostMapping("/appointment/{id}")
    public AppointmentComment addComment(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal User user) {
        return commentService.addCommentToAppointment(tenantId, id, request.getText(), user);
    }
}
