package com.tryneuro.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryneuro.backend.dto.AuthRequest;
import com.tryneuro.backend.dto.AuthResponse;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.security.JwtUtil;
import com.tryneuro.backend.service.TelegramAuthService;
import com.tryneuro.backend.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest, @RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        final User user = (User) userDetails;

        // --- АВТО-ПРИВЯЗКА TELEGRAM ID ---
        if (initData != null && !initData.isEmpty()) {
            try {
                Map<String, String> parsedData = telegramAuthService.validateAndParseData(initData);
                String userJson = parsedData.get("user");
                Map<String, Object> tgUser = objectMapper.readValue(userJson, Map.class);
                Long telegramId = Long.valueOf(tgUser.get("id").toString());
                
                if (user.getTelegramId() == null || !user.getTelegramId().equals(telegramId)) {
                    user.setTelegramId(telegramId);
                    userRepository.save(user);
                    System.out.println("LINK SUCCESS: Linked Telegram ID " + telegramId + " to user " + user.getEmail());
                }
            } catch (Exception e) {
                System.err.println("Auto-linking Telegram failed: " + e.getMessage());
            }
        }

        final String token = jwtUtil.generateToken(user, user.getTenantId(), user.getStaffId());
        return new AuthResponse(token, user.getTenantId());
    }

    @PostMapping("/telegram")
    public AuthResponse loginViaTelegram(@RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        if (initData == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing initData");

        try {
            Map<String, String> parsedData = telegramAuthService.validateAndParseData(initData);
            String userJson = parsedData.get("user");
            Map<String, Object> tgUser = objectMapper.readValue(userJson, Map.class);
            Long telegramId = Long.valueOf(tgUser.get("id").toString());

            User user = userRepository.findByTelegramId(telegramId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telegram account not linked"));

            final String token = jwtUtil.generateToken(user, user.getTenantId(), user.getStaffId());
            return new AuthResponse(token, user.getTenantId());

        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Telegram signature");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth error: " + e.getMessage());
        }
    }
}
