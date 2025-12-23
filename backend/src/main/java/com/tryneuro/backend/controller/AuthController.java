package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.LoginRequest;
import com.tryneuro.backend.dto.LoginResponse;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.security.JwtUtil;
import com.tryneuro.backend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("DEBUG: Login request for email: " + loginRequest.getEmail());
        
        try {
            // Spring Security сам проверит пароль (с учетом хеширования)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            System.out.println("DEBUG: Authentication successful");
    
            final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            
            // Нам нужно найти нашего User (entity), чтобы достать tenantId
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();
            
            final String jwt = jwtUtil.generateToken(userDetails, user.getTenantId(), user.getStaffId());
    
            return ResponseEntity.ok(new LoginResponse(jwt, user.getTenantId()));
            
        } catch (BadCredentialsException e) {
            System.out.println("DEBUG: Bad credentials for email: " + loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect username or password");
        } catch (UsernameNotFoundException e) { // Обычно ловится BadCredentialsException, но на всякий случай
             System.out.println("DEBUG: User not found: " + loginRequest.getEmail());
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            System.out.println("DEBUG: Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Auth error: " + e.getMessage());
        }
    }
}
