package com.tryneuro.backend.controller.flutter;

import com.tryneuro.backend.dto.RegisterCompanyRequest;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
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
    public Company registerCompany(@RequestBody RegisterCompanyRequest request, HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return companyService.registerCompany(request, ip, userAgent);
    }

    @GetMapping("/{id}")
    public Company getCompany(@PathVariable String id) {
        return companyService.getCompanyById(id);
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
