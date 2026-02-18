package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public List<Branch> getBranches(Authentication auth) {
        return branchService.getBranches(auth.getName());
    }

    @PostMapping
    public Branch createBranch(@RequestBody Branch branch, Authentication auth) {
        return branchService.createBranch(branch, auth.getName());
    }

    @PutMapping("/{id}")
    public Branch updateBranch(@PathVariable String id, @RequestBody Branch branch, Authentication auth) {
        return branchService.updateBranch(id, branch, auth.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable String id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok().build();
    }
}
