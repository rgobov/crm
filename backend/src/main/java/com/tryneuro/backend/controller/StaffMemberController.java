package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.CreateStaffRequest;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.service.StaffMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffMemberController {
    private final StaffMemberService staffMemberService;

    @Autowired
    public StaffMemberController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @GetMapping
    public List<StaffMember> getAllStaff(@RequestAttribute("tenantId") String tenantId) {
        return staffMemberService.getAllStaff(tenantId);
    }

    @PostMapping
    public StaffMember createStaffMember(@RequestAttribute("tenantId") String tenantId, @RequestBody CreateStaffRequest request) {
        return staffMemberService.addStaffMember(request, tenantId);
    }
    
    // Добавили метод для обновления (PUT)
    @PutMapping("/{id}")
    public StaffMember updateStaffMember(@RequestAttribute("tenantId") String tenantId, @PathVariable String id, @RequestBody CreateStaffRequest request) {
        return staffMemberService.updateStaffMember(id, request, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }
}
