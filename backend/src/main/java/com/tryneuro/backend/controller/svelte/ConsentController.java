package com.tryneuro.backend.controller.svelte;

import com.tryneuro.backend.dto.ConsentDto;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
    }

    @GetMapping
    public List<ConsentDto> getMyConsents(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return consentService.getAllConsents(user.getId());
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeConsent(@PathVariable String id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        consentService.revokeConsent(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
