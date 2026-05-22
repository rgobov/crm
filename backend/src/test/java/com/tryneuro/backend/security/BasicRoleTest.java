package com.tryneuro.backend.security;

import com.tryneuro.backend.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicRoleTest {

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
        
        // Проверяем количество ролей
        assertEquals(4, UserRole.values().length);
        
        System.out.println("✅ UserRole enum работает корректно");
    }

    @Test
    void testRoleValues() {
        UserRole[] roles = UserRole.values();
        
        assertTrue(containsRole(roles, UserRole.ADMIN));
        assertTrue(containsRole(roles, UserRole.MANAGER));
        assertTrue(containsRole(roles, UserRole.EMPLOYEE));
        assertTrue(containsRole(roles, UserRole.CLIENT));
        
        System.out.println("✅ Все роли доступны в enum");
    }
    
    private boolean containsRole(UserRole[] roles, UserRole role) {
        for (UserRole r : roles) {
            if (r == role) return true;
        }
        return false;
    }
}
