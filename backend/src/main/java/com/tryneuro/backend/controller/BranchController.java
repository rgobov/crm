package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public List<Branch> getBranches(Authentication auth, @RequestAttribute("tenantId") String tenantId) {
        log.info("--- DEBUG BRANCHES ---");
        log.info("User Authenticated: {}", auth != null ? auth.getName() : "NULL");
        log.info("Extracted tenantId from RequestAttribute: {}", tenantId);
        
        List<Branch> branches = branchService.getBranches(tenantId);
        
        log.info("Branches found in DB for this tenantId: {}", branches.size());
        if (!branches.isEmpty()) {
            branches.forEach(b -> log.info("Branch: ID={}, Name={}, Tenant={}", b.getId(), b.getName(), b.getTenantId()));
        }
        log.info("-----------------------");
        
        return branches;
    }

    @PostMapping
    public Branch createBranch(@RequestBody Branch branch, @RequestAttribute("tenantId") String tenantId) {
        log.info("Creating branch for tenantId: {}", tenantId);
        return branchService.createBranch(branch, tenantId);
    }

    @PutMapping("/{id}")
    public Branch updateBranch(@PathVariable String id, @RequestBody Branch branch, @RequestAttribute("tenantId") String tenantId) {
        return branchService.updateBranch(id, branch, tenantId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable String id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok().build();
    }
}
