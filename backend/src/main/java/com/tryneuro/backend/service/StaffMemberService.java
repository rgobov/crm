package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public StaffMemberService(StaffMemberRepository staffMemberRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.staffMemberRepository = staffMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private LocalTime parseTime(String time) {
        if (time == null || time.isEmpty()) return null;
        try {
            return LocalTime.parse(time, timeFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    public List<StaffMember> getAllStaff(String tenantId) {
        List<StaffMember> staffMembers = staffMemberRepository.findByTenantId(tenantId);
        for (StaffMember staff : staffMembers) {
            userRepository.findByStaffId(staff.getId()).ifPresent(user -> {
                staff.setRole(user.getRole().name());
                staff.setEmail(user.getEmail());
            });
        }
        return staffMembers;
    }

    @Transactional
    public StaffMember addStaffMember(CreateStaffRequest request, String tenantId) {
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с email " + request.getEmail() + " уже существует.");
            });
        }

        StaffMember staffMember = new StaffMember();
        staffMember.setName(request.getName());
        staffMember.setSpecialty(request.getSpecialty());
        staffMember.setTenantId(tenantId);
        staffMember.setAvailable(request.isAvailable());
        staffMember.setWorkStartTime(parseTime(request.getWorkStartTime()));
        staffMember.setWorkEndTime(parseTime(request.getWorkEndTime()));
        staffMember.setBreakStartTime(parseTime(request.getBreakStartTime()));
        staffMember.setBreakEndTime(parseTime(request.getBreakEndTime()));
        
        StaffMember savedStaff = staffMemberRepository.save(staffMember);

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setTenantId(tenantId);
            user.setStaffId(savedStaff.getId());
            user.setRole("MANAGER".equalsIgnoreCase(request.getRole()) ? UserRole.MANAGER : UserRole.EMPLOYEE);
            userRepository.save(user);
            savedStaff.setRole(user.getRole().name());
            savedStaff.setEmail(user.getEmail());
        }

        return savedStaff;
    }
    
    @Transactional
    public StaffMember updateStaffMember(String id, CreateStaffRequest request, String tenantId) {
        StaffMember staffMember = staffMemberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сотрудник не найден"));

        if (!staffMember.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещен");
        }

        staffMember.setName(request.getName());
        staffMember.setSpecialty(request.getSpecialty());
        staffMember.setAvailable(request.isAvailable());
        staffMember.setWorkStartTime(parseTime(request.getWorkStartTime()));
        staffMember.setWorkEndTime(parseTime(request.getWorkEndTime()));
        staffMember.setBreakStartTime(parseTime(request.getBreakStartTime()));
        staffMember.setBreakEndTime(parseTime(request.getBreakEndTime()));
        
        StaffMember savedStaff = staffMemberRepository.save(staffMember);
        
        userRepository.findByStaffId(id).ifPresent(user -> {
            boolean userNeedsUpdate = false;

            if (request.getRole() != null) {
                user.setRole("MANAGER".equalsIgnoreCase(request.getRole()) ? UserRole.MANAGER : UserRole.EMPLOYEE);
                userNeedsUpdate = true;
            }

            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                user.setEmail(request.getEmail());
                userNeedsUpdate = true;
            }

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userNeedsUpdate = true;
            }

            if (userNeedsUpdate) {
                userRepository.save(user);
            }

            savedStaff.setRole(user.getRole().name());
            savedStaff.setEmail(user.getEmail());
        });

        return savedStaff;
    }

    public void deleteStaffMember(String id) {
        userRepository.findByStaffId(id).ifPresent(userRepository::delete);
        staffMemberRepository.deleteById(id);
    }
}
