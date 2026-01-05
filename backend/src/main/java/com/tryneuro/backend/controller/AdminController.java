package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff") // НОВЫЙ ПУТЬ
public class AdminController {
    private final StaffMemberService staffMemberService;

    @Autowired
    public AdminController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    // Администратор получает ВСЕХ сотрудников
    @GetMapping
    public List<StaffMember> getAllStaff(@RequestAttribute("tenantId") String tenantId) {
        return staffMemberService.getAllStaff(tenantId);
    }

    @PostMapping
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, tenantId);
    }
    
    @PutMapping("/{id}")
    public StaffMember updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return staffMemberService.updateStaffMember(id, request, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }
}
