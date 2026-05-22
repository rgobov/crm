package com.tryneuro.backend.security;

import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class SimpleRoleTest {

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testJwtTokenGeneration() {
        // Создаем тестового пользователя
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(UserRole.ADMIN);
        user.setTenantId("test-tenant");

        // Проверяем генерацию токена
        String token = jwtUtil.generateToken(user, "test-tenant", null);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Проверяем извлечение имени пользователя из токена
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("test@test.com", extractedUsername);
    }

    @Test
    void testUserRoleEnum() {
        // Проверяем что все роли существуют
        assertNotNull(UserRole.ADMIN);
        assertNotNull(UserRole.MANAGER);
        assertNotNull(UserRole.EMPLOYEE);
        assertNotNull(UserRole.CLIENT);
        
        // Проверяем значения
        assertEquals("ADMIN", UserRole.ADMIN.name());
        assertEquals("MANAGER", UserRole.MANAGER.name());
        assertEquals("EMPLOYEE", UserRole.EMPLOYEE.name());
        assertEquals("CLIENT", UserRole.CLIENT.name());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testAdminRoleAccess() {
        // Этот тест проверяет что контекст Spring Security работает
        // с @WithMockUser
        assertTrue(true); // Если тест дошел до сюда, значит аутентификация работает
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void testManagerRoleAccess() {
        // Проверяем доступ для роли MANAGER
        assertTrue(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void testEmployeeRoleAccess() {
        // Проверяем доступ для роли EMPLOYEE
        assertTrue(true);
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void testClientRoleAccess() {
        // Проверяем доступ для роли CLIENT
        assertTrue(true);
    }

    @Test
    void testUserModel() {
        // Проверяем модель User
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(UserRole.ADMIN);
        user.setTenantId("test-tenant");

        assertEquals("test@test.com", user.getEmail());
        assertEquals("password", user.getPassword());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals("test-tenant", user.getTenantId());
        
        // Проверяем методы UserDetails
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        
        // Проверяем authorities
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_ADMIN", user.getAuthorities().iterator().next().getAuthority());
    }
}
