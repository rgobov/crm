package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.RegisterClientRequest;
import com.tryneuro.backend.dto.RegisterCompanyRequest;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.CompanyRepository;
import com.tryneuro.backend.repository.ContactRepository;
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
    private final ContactRepository contactRepository;

    @Autowired
    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, ContactRepository contactRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactRepository = contactRepository;
    }

    @Transactional
    public Company registerCompany(RegisterCompanyRequest request) {
        if (request.getAdminEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email не может быть пустым");
        }
        String normalizedEmail = request.getAdminEmail().trim().toLowerCase();

        // Проверяем, существует ли пользователь с таким email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким email уже существует");
        }

        // 1. Создаем компанию
        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setAddress(request.getCompanyAddress());
        company.setOwnerEmail(normalizedEmail);
        
        Company savedCompany = companyRepository.save(company);

        // 2. Создаем пользователя-админа, привязанного к этой компании
        User admin = new User();
        admin.setEmail(normalizedEmail);
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

    @Transactional
    public User registerClient(RegisterClientRequest request) {
        Company company = companyRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Указанная компания не найдена"));

        if (request.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email не может быть пустым");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким email уже существует");
        }

        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setEmail(normalizedEmail);
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            contact.setPhones(java.util.List.of(request.getPhone()));
        } else {
            contact.setPhones(java.util.List.of());
        }
        contact.setTenantId(company.getId());
        contact = contactRepository.save(contact);

        User clientUser = new User();
        clientUser.setEmail(normalizedEmail);
        clientUser.setPassword(passwordEncoder.encode(request.getPassword()));
        clientUser.setRole(UserRole.CLIENT);
        clientUser.setTenantId(company.getId());
        clientUser.setContactId(contact.getId());

        return userRepository.save(clientUser);
    }
}
