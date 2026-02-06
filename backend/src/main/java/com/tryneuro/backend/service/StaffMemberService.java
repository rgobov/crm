package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import com.tryneuro.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public StaffMemberService(StaffMemberRepository staffMemberRepository,
                              StaffShiftRepository staffShiftRepository,
                              UserRepository userRepository,
                              AppointmentRepository appointmentRepository,
                              PasswordEncoder passwordEncoder) {
        this.staffMemberRepository = staffMemberRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<StaffMember> getStaffMemberById(String id) {
        Optional<StaffMember> staffOpt = staffMemberRepository.findById(id);
        staffOpt.ifPresent(this::enrichWithUserData);
        return staffOpt;
    }

    // ОБНОВЛЕНО: Добавлена поддержка параметра active
    public Page<StaffMember> getStaffPaged(String tenantId, String query, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("active").descending().and(Sort.by("name").ascending()));
        Page<StaffMember> staffPage = staffMemberRepository.findByTenantIdAndQuery(tenantId, query, active, pageable);
        staffPage.forEach(this::enrichWithUserData);
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

        StaffMember saved = staffMemberRepository.save(staffMember);

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setEmail(request.getEmail());
            newUser.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "qwerty"));
            newUser.setRole("MANAGER".equalsIgnoreCase(request.getRole()) ? UserRole.MANAGER : UserRole.EMPLOYEE);
            newUser.setTenantId(tenantId);
            newUser.setStaffId(saved.getId());
            userRepository.save(newUser);
        }

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

        StaffMember savedStaff = staffMemberRepository.save(staffMember);
        
        userRepository.findByStaffId(id).ifPresent(user -> {
            if (request.getRole() != null) user.setRole("MANAGER".equalsIgnoreCase(request.getRole()) ? UserRole.MANAGER : UserRole.EMPLOYEE);
            if (request.getEmail() != null && !request.getEmail().isEmpty()) user.setEmail(request.getEmail());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        });

        enrichWithUserData(savedStaff);
        return savedStaff;
    }

    @Transactional
    public void deleteStaffMember(String id) {
        userRepository.findByStaffId(id).ifPresent(userRepository::delete);
        boolean hasAppointments = appointmentRepository.existsByStaffMemberId(id);

        if (hasAppointments) {
            staffMemberRepository.findById(id).ifPresent(staff -> {
                staff.setActive(false);
                staffMemberRepository.save(staff);
            });
        } else {
            staffMemberRepository.deleteById(id);
        }
    }

    public List<StaffMember> getAllStaff(String tenantId) {
        return staffMemberRepository.findByTenantId(tenantId).stream()
                .map(s -> { enrichWithUserData(s); return s; })
                .collect(Collectors.toList());
    }

    public Optional<StaffMember> getStaffByIdAndDate(String id, LocalDate date) {
        return getStaffMemberById(id).map(staff -> {
            staffShiftRepository.findByStaffIdAndDate(staff.getId(), date).ifPresent(shift -> {
                staff.setDayOff(shift.isDayOff());
                staff.setWorkStartTime(shift.getWorkStartTime());
                staff.setWorkEndTime(shift.getWorkEndTime());
                staff.setBreakStartTime(shift.getBreakStartTime());
                staff.setBreakEndTime(shift.getBreakEndTime());
            });
            return staff;
        });
    }

    public List<StaffMember> getStaffForDate(String tenantId, LocalDate date) {
        return staffMemberRepository.findByTenantId(tenantId).stream()
                .filter(StaffMember::isActive)
                .map(staff -> {
                    enrichWithUserData(staff);
                    staffShiftRepository.findByStaffIdAndDate(staff.getId(), date).ifPresentOrElse(shift -> {
                        staff.setDayOff(shift.isDayOff());
                        staff.setWorkStartTime(shift.getWorkStartTime());
                        staff.setWorkEndTime(shift.getWorkEndTime());
                        staff.setBreakStartTime(shift.getBreakStartTime());
                        staff.setBreakEndTime(shift.getBreakEndTime());
                    }, () -> staff.setDayOff(true));
                    return staff;
                }).collect(Collectors.toList());
    }

    @Transactional
    public StaffShift saveShift(StaffShift shift) {
        return staffShiftRepository.findByStaffIdAndDate(shift.getStaffId(), shift.getDate())
                .map(existing -> {
                    shift.setId(existing.getId());
                    return staffShiftRepository.save(shift);
                })
                .orElseGet(() -> staffShiftRepository.save(shift));
    }
}
