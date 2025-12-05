package com.tryneuro.backend.controller;

import com.tryneuro.backend.dto.RegisterCompanyRequest;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/register")
    public ResponseEntity<Company> registerCompany(@RequestBody RegisterCompanyRequest request) {
        Company company = companyService.registerCompany(request);
        return ResponseEntity.ok(company);
    }
}
