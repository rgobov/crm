package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<Branch> getBranches(String tenantId) {
        return branchRepository.findByTenantId(tenantId);
    }

    public Branch createBranch(Branch branch, String tenantId) {
        branch.setTenantId(tenantId);
        return branchRepository.save(branch);
    }

    public Branch updateBranch(String id, Branch branch, String tenantId) {
        branch.setId(id);
        branch.setTenantId(tenantId);
        return branchRepository.save(branch);
    }

    public void deleteBranch(String id) {
        branchRepository.deleteById(id);
    }
}
