package com.tryneuro.backend.service;

import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;

    @Autowired
    public StaffMemberService(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    public List<StaffMember> getAllStaff(String tenantId) {
        return staffMemberRepository.findByTenantId(tenantId);
    }

    public StaffMember addStaffMember(StaffMember staffMember, String tenantId) {
        staffMember.setTenantId(tenantId);
        return staffMemberRepository.save(staffMember);
    }

    public void deleteStaffMember(String id) {
        staffMemberRepository.deleteById(id);
    }
}
