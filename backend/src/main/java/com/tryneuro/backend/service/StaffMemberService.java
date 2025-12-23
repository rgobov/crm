package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StaffMemberService(StaffMemberRepository staffMemberRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.staffMemberRepository = staffMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StaffMember> getAllStaff(String tenantId) {
        List<StaffMember> staffMembers = staffMemberRepository.findByTenantId(tenantId);
        
        // Для каждого сотрудника ищем его пользователя и заполняем роль
        for (StaffMember staff : staffMembers) {
            Optional<User> userOpt = userRepository.findByStaffId(staff.getId());
            if (userOpt.isPresent()) {
                staff.setRole(userOpt.get().getRole().name());
            } else {
                staff.setRole("NONE"); // Или null, если аккаунта нет
            }
        }
        
        return staffMembers;
    }

    @Transactional
    public StaffMember addStaffMember(CreateStaffRequest request, String tenantId) {
        // ... (код создания остается тем же, см. ниже полное обновление) ...
        // Но так как я перезаписываю файл, мне нужно вернуть код addStaffMember на место.
        // И добавить метод updateStaffMemberRole
        
        // 1. Создаем сотрудника (StaffMember)
        StaffMember staffMember = new StaffMember();
        staffMember.setName(request.getName());
        staffMember.setSpecialty(request.getSpecialty());
        staffMember.setTenantId(tenantId);
        
        StaffMember savedStaff = staffMemberRepository.save(staffMember);

        // 2. Если переданы данные для входа
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            request.getPassword() != null && !request.getPassword().isEmpty()) {
            
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setTenantId(tenantId);
            user.setStaffId(savedStaff.getId());

            if ("MANAGER".equalsIgnoreCase(request.getRole())) {
                user.setRole(UserRole.MANAGER);
            } else {
                user.setRole(UserRole.EMPLOYEE);
            }

            userRepository.save(user);
            savedStaff.setRole(user.getRole().name()); // Возвращаем роль клиенту
        } else {
            savedStaff.setRole("NONE");
        }

        return savedStaff;
    }
    
    @Transactional
    public StaffMember updateStaffMember(String id, CreateStaffRequest request, String tenantId) {
        StaffMember staffMember = staffMemberRepository.findById(id).orElseThrow(() -> new RuntimeException("Staff not found"));
        staffMember.setName(request.getName());
        staffMember.setSpecialty(request.getSpecialty());
        StaffMember savedStaff = staffMemberRepository.save(staffMember);
        
        // Ищем пользователя, связанного с этим сотрудником
        Optional<User> userOpt = userRepository.findByStaffId(id);
        
        if (request.getRole() != null) {
            UserRole newRole = "MANAGER".equalsIgnoreCase(request.getRole()) ? UserRole.MANAGER : UserRole.EMPLOYEE;
            
            if (userOpt.isPresent()) {
                // Если пользователь уже есть - обновляем роль
                User user = userOpt.get();
                user.setRole(newRole);
                userRepository.save(user);
                savedStaff.setRole(newRole.name());
            } else {
                // Если пользователя не было, но роль передали...
                // Тут сложнее: у нас может не быть email/password для создания нового пользователя.
                // В рамках этой задачи предположим, что мы меняем роль только если пользователь уже есть.
                // Или если email/pass переданы, создаем нового (логика аналогична create).
                if (request.getEmail() != null && request.getPassword() != null) {
                     User newUser = new User();
                     newUser.setEmail(request.getEmail());
                     newUser.setPassword(passwordEncoder.encode(request.getPassword()));
                     newUser.setTenantId(tenantId);
                     newUser.setStaffId(id);
                     newUser.setRole(newRole);
                     userRepository.save(newUser);
                     savedStaff.setRole(newRole.name());
                } else {
                    savedStaff.setRole("NONE"); 
                }
            }
        } else if (userOpt.isPresent()) {
             savedStaff.setRole(userOpt.get().getRole().name());
        } else {
             savedStaff.setRole("NONE");
        }
        
        return savedStaff;
    }

    public void deleteStaffMember(String id) {
        // Удаляем пользователя перед удалением сотрудника
        Optional<User> userOpt = userRepository.findByStaffId(id);
        userOpt.ifPresent(userRepository::delete);
        
        staffMemberRepository.deleteById(id);
    }
}
