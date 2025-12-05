package com.tryneuro.backend.controller;

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
    public List<StaffMember> getAllStaff(@RequestHeader("X-Tenant-ID") String tenantId) {
        return staffMemberService.getAllStaff(tenantId);
    }

    @PostMapping
    public StaffMember createStaffMember(@RequestHeader("X-Tenant-ID") String tenantId, @RequestBody StaffMember staffMember) {
        return staffMemberService.addStaffMember(staffMember, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteStaffMember(@PathVariable String id) {
        staffMemberService.deleteStaffMember(id);
    }
}
