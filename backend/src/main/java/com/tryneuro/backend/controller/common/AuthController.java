package com.tryneuro.backend.controller.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryneuro.backend.dto.AuthRequest;
import com.tryneuro.backend.dto.AuthResponse;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.security.JwtUtil;
import com.tryneuro.backend.service.TelegramAuthService;
import com.tryneuro.backend.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final TelegramAuthService telegramAuthService;
    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest, @RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData) {
        // ТЕХНИЧЕСКОЕ РЕШЕНИЕ: Приводим email к нижнему регистру для надежности в эмуляторах
        String normalizedEmail = authRequest.getEmail().trim().toLowerCase();
        log.info("AUTH: Login attempt for email: {}", normalizedEmail);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, authRequest.getPassword())
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedEmail);
            final User user = (User) userDetails;

            if (user.getStaffId() != null) {
                StaffMember staff = staffMemberRepository.findById(user.getStaffId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Профиль сотрудника удален"));
                if (!staff.isActive()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ваш аккаунт заблокирован");
                }
            }

            final String token = jwtUtil.generateToken(user, user.getTenantId(), user.getStaffId());
            log.info("AUTH: Login successful for {}, tenant: {}", normalizedEmail, user.getTenantId());
            return new AuthResponse(token, user.getTenantId());

        } catch (BadCredentialsException e) {
            log.warn("AUTH: Invalid credentials for {}", normalizedEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        } catch (Exception e) {
            log.error("AUTH: Unexpected error during login", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка сервера");
        }
    }

    @GetMapping("/me")
    public User getCurrentUser(@RequestAttribute(value = "tenantId", required = false) String tenantId) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия истекла");
        }
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
    }
}
