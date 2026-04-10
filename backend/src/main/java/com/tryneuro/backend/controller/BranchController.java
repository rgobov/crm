package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.BranchDto;
import com.tryneuro.backend.dto.DtoMapper;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<List<BranchDto>> getBranches(Authentication auth, @RequestAttribute("tenantId") String tenantId) {
        log.info("--- DEBUG BRANCHES ---");
        log.info("User Authenticated: {}", auth != null ? auth.getName() : "NULL");
        log.info("Extracted tenantId from RequestAttribute: {}", tenantId);
        
        List<Branch> branches = branchService.getBranches(tenantId);
        
        log.info("Branches found in DB for this tenantId: {}", branches.size());
        
        List<BranchDto> result = branches.stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok()
                .body(result);
    }

    @PostMapping
    public ResponseEntity<BranchDto> createBranch(@RequestBody BranchDto branchDto, @RequestAttribute("tenantId") String tenantId) {
        log.info("Creating branch for tenantId: {}", tenantId);
        Branch branch = DtoMapper.toEntity(branchDto, tenantId);
        return ResponseEntity.ok()
                .body(DtoMapper.toDto(branchService.createBranch(branch, tenantId)));
    }

    @PutMapping("/{id}")
    public BranchDto updateBranch(@PathVariable String id, @RequestBody BranchDto branchDto, @RequestAttribute("tenantId") String tenantId) {
        Branch branch = DtoMapper.toEntity(branchDto, tenantId);
        return DtoMapper.toDto(branchService.updateBranch(id, branch, tenantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable String id) {
        try {
            branchService.deleteBranch(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
