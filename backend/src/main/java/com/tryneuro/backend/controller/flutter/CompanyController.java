package com.tryneuro.backend.controller.flutter;

import com.tryneuro.backend.dto.RegisterCompanyRequest;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/register")
    public Company registerCompany(@RequestBody RegisterCompanyRequest request) {
        return companyService.registerCompany(request);
    }

    @GetMapping("/{id}")
    public Company getCompany(@PathVariable String id) {
        return companyService.getCompanyById(id);
    }
}
