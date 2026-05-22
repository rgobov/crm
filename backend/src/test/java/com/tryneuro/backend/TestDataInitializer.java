package com.tryneuro.backend;

import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Инициализация тестовых данных для тестового профиля
 */
@Configuration
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initTestData(UserRepository userRepository) {
        return args -> {
            log.info("🚀 Инициализация тестовых данных...");
            
            // Создаем тестовых пользователей если их нет
            if (userRepository.count() == 0) {
                createTestUsers(userRepository);
                log.info("✅ Тестовые данные успешно созданы");
            } else {
                log.info("ℹ️ Тестовые данные уже существуют");
            }
        };
    }

    private void createTestUsers(UserRepository userRepository) {
        // Создаем админа
        User admin = createTestUser(
            "admin@test.com",
            "password",
            UserRole.ADMIN,
            "test-tenant"
        );
        userRepository.save(admin);
        log.info("✅ Создан администратор: admin@test.com");

        // Создаем менеджера
        User manager = createTestUser(
            "manager@test.com",
            "password",
            UserRole.MANAGER,
            "test-tenant"
        );
        userRepository.save(manager);
        log.info("✅ Создан менеджер: manager@test.com");

        // Создаем сотрудника
        User employee = createTestUser(
            "employee@test.com",
            "password",
            UserRole.EMPLOYEE,
            "test-tenant"
        );
        userRepository.save(employee);
        log.info("✅ Создан сотрудник: employee@test.com");

        log.info("✅ Все тестовые пользователи созданы");
    }

    private User createTestUser(String email, String password, UserRole role, String tenantId) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setTenantId(tenantId);
        return user;
    }
}
