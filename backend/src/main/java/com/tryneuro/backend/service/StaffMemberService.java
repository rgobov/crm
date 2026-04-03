package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.BranchRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import com.tryneuro.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public StaffMemberService(StaffMemberRepository staffMemberRepository,
                              StaffShiftRepository staffShiftRepository,
                              UserRepository userRepository,
                              AppointmentRepository appointmentRepository,
                              BranchRepository branchRepository,
                              PasswordEncoder passwordEncoder,
                              SimpMessagingTemplate messagingTemplate) {
        this.staffMemberRepository = staffMemberRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Optional<StaffMember> getStaffMemberById(String id) {
        Optional<StaffMember> staffOpt = staffMemberRepository.findById(id);
        staffOpt.ifPresent(staff -> {
            if (staff.getBranches() != null) {
                staff.getBranches().size();
            }
            enrichWithUserData(staff);
        });
        return staffOpt;
    }

    @Transactional
    public Page<StaffMember> getStaffPaged(String tenantId, String query, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("active").descending().and(Sort.by("name").ascending()));
        Page<StaffMember> staffPage = staffMemberRepository.findByTenantIdAndQuery(tenantId, query, active, pageable);
        staffPage.forEach(staff -> {
            if (staff.getBranches() != null) {
                staff.getBranches().size();
            }
            enrichWithUserData(staff);
        });
        return staffPage;
    }

    private void enrichWithUserData(StaffMember staff) {
        userRepository.findByStaffId(staff.getId()).ifPresent(user -> {
            staff.setRole(user.getRole().name());
            staff.setEmail(user.getEmail());
        });
    }

    @Transactional
    public StaffMember addStaffMember(CreateStaffRequest request, String tenantId) {
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже занят");
            });
        }
        StaffMember staffMember = new StaffMember();
        staffMember.setName(request.getName());
        staffMember.setSpecialty(request.getSpecialty());
        staffMember.setPhone(request.getPhone());
        staffMember.setTenantId(tenantId);
        staffMember.setActive(true);
        if (request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
            Set<Branch> branches = new HashSet<>(branchRepository.findAllById(request.getBranchIds()));
            staffMember.setBranches(branches);
        }
        StaffMember saved = staffMemberRepository.save(staffMember);
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setEmail(request.getEmail());
            newUser.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "qwerty"));
            
            try {
                newUser.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
            } catch (Exception e) {
                newUser.setRole(UserRole.EMPLOYEE);
            }
            
            newUser.setTenantId(tenantId);
            newUser.setStaffId(saved.getId());
            userRepository.save(newUser);
        }
        
        // ✅ Уведомляем другие клиенты о появлении нового сотрудника
        notifyChange(tenantId);
        
        enrichWithUserData(saved);
        return saved;
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
        staffMember.setPhone(request.getPhone());
        staffMember.setActive(request.isAvailable());
        if (request.getBranchIds() != null) {
            Set<Branch> branches = new HashSet<>(branchRepository.findAllById(request.getBranchIds()));
            staffMember.setBranches(branches);
        }
        StaffMember savedStaff = staffMemberRepository.save(staffMember);
        
        User user = userRepository.findByStaffId(id).orElseGet(() -> {
            if (request.getEmail() == null || request.getEmail().isEmpty()) return null;
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setStaffId(id);
            newUser.setTenantId(tenantId);
            return newUser;
        });

        if (user != null) {
            if (request.getRole() != null) {
                try {
                    user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
                } catch (Exception e) {
                    user.setRole(UserRole.EMPLOYEE);
                }
            }
            if (request.getEmail() != null && !request.getEmail().isEmpty()) user.setEmail(request.getEmail());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            } else if (user.getPassword() == null) {
                user.setPassword(passwordEncoder.encode("qwerty"));
            }
            userRepository.save(user);
        }
        
        // ✅ Уведомляем другие клиенты об изменении данных или роли сотрудника
        notifyChange(tenantId);
        
        enrichWithUserData(savedStaff);
        return savedStaff;
    }

    @Transactional
    public void deleteStaffMember(String id) {
        staffMemberRepository.findById(id).ifPresent(staff -> {
            userRepository.findByStaffId(id).ifPresent(userRepository::delete);
            boolean hasAppointments = appointmentRepository.existsByStaffMemberId(id);
            if (hasAppointments) {
                staff.setActive(false);
                staffMemberRepository.save(staff);
            } else {
                staffMemberRepository.deleteById(id);
            }
            // ✅ Уведомляем об удалении сотрудника
            notifyChange(staff.getTenantId());
        });
    }

    @Transactional
    public List<StaffMember> getAllStaff(String tenantId) {
        return staffMemberRepository.findByTenantId(tenantId).stream()
                .peek(staff -> {
                    if (staff.getBranches() != null) staff.getBranches().size();
                    enrichWithUserData(staff);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<StaffMember> getStaffByIdAndDate(String id, LocalDate date) {
        return getStaffMemberById(id).map(staff -> {
            List<StaffShift> shifts = staffShiftRepository.findByStaffIdAndDate(staff.getId(), date);
            if (!shifts.isEmpty()) {
                StaffShift shift = shifts.get(0);
                staff.setDayOff(shift.isDayOff());
                staff.setWorkStartTime(shift.getWorkStartTime());
                staff.setWorkEndTime(shift.getWorkEndTime());
                staff.setBreakStartTime(shift.getBreakStartTime());
                staff.setBreakEndTime(shift.getBreakEndTime());
            }
            return staff;
        });
    }

    @Transactional
    public List<StaffMember> getStaffForDate(String tenantId, LocalDate date, String branchId) {
        List<StaffMember> staffList;
        if (branchId != null && !branchId.isEmpty() && !"null".equals(branchId)) {
            staffList = staffMemberRepository.findByTenantIdAndBranchId(tenantId, branchId);
        } else {
            staffList = staffMemberRepository.findByTenantId(tenantId);
        }
        return staffList.stream()
                .filter(StaffMember::isActive)
                .peek(staff -> {
                    if (staff.getBranches() != null) staff.getBranches().size();
                    enrichWithUserData(staff);
                    staffShiftRepository.findByStaffIdAndDateAndBranchId(staff.getId(), date, branchId).ifPresentOrElse(shift -> {
                        staff.setDayOff(shift.isDayOff());
                        staff.setWorkStartTime(shift.getWorkStartTime());
                        staff.setWorkEndTime(shift.getWorkEndTime());
                        staff.setBreakStartTime(shift.getBreakStartTime());
                        staff.setBreakEndTime(shift.getBreakEndTime());
                    }, () -> staff.setDayOff(true));
                }).collect(Collectors.toList());
    }

    @Transactional
    public StaffShift saveShift(StaffShift shift) {
        StaffShift saved = staffShiftRepository.findByStaffIdAndDateAndBranchId(shift.getStaffId(), shift.getDate(), shift.getBranchId())
                .map(existing -> {
                    existing.setWorkStartTime(shift.getWorkStartTime());
                    existing.setWorkEndTime(shift.getWorkEndTime());
                    existing.setBreakStartTime(shift.getBreakStartTime());
                    existing.setBreakEndTime(shift.getBreakEndTime());
                    existing.setDayOff(shift.isDayOff());
                    return staffShiftRepository.save(existing);
                })
                .orElseGet(() -> staffShiftRepository.save(shift));

        notifyChange(saved.getTenantId());
        return saved;
    }

    private void notifyChange(String tenantId) {
        if (tenantId != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SCHEDULE_UPDATED");
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
        }
    }
}
