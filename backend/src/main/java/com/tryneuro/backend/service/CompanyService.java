package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.RegisterCompanyRequest;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.CompanyRepository;
import com.tryneuro.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Company registerCompany(RegisterCompanyRequest request) {
        // 1. Создаем компанию
        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setAddress(request.getCompanyAddress());
        company.setOwnerEmail(request.getAdminEmail());
        
        Company savedCompany = companyRepository.save(company);

        // 2. Создаем пользователя-админа, привязанного к этой компании
        User admin = new User();
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(request.getAdminPassword())); // Хешируем пароль
        admin.setRole(UserRole.ADMIN);
        admin.setTenantId(savedCompany.getId());

        userRepository.save(admin);

        return savedCompany;
    }

    public Company getCompanyById(String id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Компания не найдена"));
    }
}
