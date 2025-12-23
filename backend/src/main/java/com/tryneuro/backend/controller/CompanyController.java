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
        System.out.println("DEBUG: --- NEW REGISTER REQUEST ---");
        System.out.println("DEBUG: Name: " + request.getCompanyName());
        System.out.println("DEBUG: Address: " + request.getCompanyAddress());
        System.out.println("DEBUG: Admin Email: " + request.getAdminEmail());
        System.out.println("DEBUG: Password: " + (request.getAdminPassword() != null ? "***" : "NULL"));
        
        try {
            Company company = companyService.registerCompany(request);
            System.out.println("DEBUG: Service call finished successfully. Company ID: " + company.getId());
            return ResponseEntity.ok(company);
        } catch (Exception e) {
            System.out.println("DEBUG: ERROR in registerCompany: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
