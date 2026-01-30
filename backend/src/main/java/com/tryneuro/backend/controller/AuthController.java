package com.tryneuro.backend.controller;

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

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final TelegramAuthService telegramAuthService;
    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository; // Добавлено для проверки активности
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest, @RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
            final User user = (User) userDetails;

            // КРИТИЧЕСКАЯ ПРОВЕРКА: Если это сотрудник, проверяем активен ли он
            if (user.getStaffId() != null) {
                StaffMember staff = staffMemberRepository.findById(user.getStaffId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Профиль сотрудника удален"));
                if (!staff.isActive()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ваш аккаунт заблокирован");
                }
            }

            if (initData != null && !initData.isEmpty()) {
                linkTelegram(initData, user);
            }

            final String token = jwtUtil.generateToken(user, user.getTenantId(), user.getStaffId());
            return new AuthResponse(token, user.getTenantId());

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        }
    }

    @PostMapping("/telegram")
    public AuthResponse loginViaTelegram(@RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        try {
            Map<String, String> parsedData = telegramAuthService.validateAndParseData(initData);
            String userJson = parsedData.get("user");
            Map<String, Object> tgUser = objectMapper.readValue(userJson, Map.class);
            Long telegramId = Long.valueOf(tgUser.get("id").toString());

            User user = userRepository.findByTelegramId(telegramId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Аккаунт не связан с Telegram"));

            // КРИТИЧЕСКАЯ ПРОВЕРКА: Если это сотрудник, проверяем активен ли он
            if (user.getStaffId() != null) {
                StaffMember staff = staffMemberRepository.findById(user.getStaffId()).orElse(null);
                if (staff == null || !staff.isActive()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ заблокирован");
                }
            }

            final String token = jwtUtil.generateToken(user, user.getTenantId(), user.getStaffId());
            return new AuthResponse(token, user.getTenantId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ошибка входа через Telegram");
        }
    }

    private void linkTelegram(String initData, User user) {
        try {
            Map<String, String> parsedData = telegramAuthService.validateAndParseData(initData);
            String userJson = parsedData.get("user");
            Map<String, Object> tgUser = objectMapper.readValue(userJson, Map.class);
            Long telegramId = Long.valueOf(tgUser.get("id").toString());

            if (user.getTelegramId() == null || !user.getTelegramId().equals(telegramId)) {
                user.setTelegramId(telegramId);
                userRepository.save(user);
            }
        } catch (Exception e) {
            System.err.println("Auto-linking failed: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public User getCurrentUser(@RequestAttribute("tenantId") String tenantId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
