package com.tryneuro.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryneuro.backend.model.User;
import com.tryneuro.backend.model.UserRole;
import com.tryneuro.backend.model.Company;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.repository.UserRepository;
import com.tryneuro.backend.repository.CompanyRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.service.DashboardService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import com.tryneuro.backend.service.WappiService;
import com.tryneuro.backend.security.JwtAuthenticationFilter;
import com.tryneuro.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class RoleBasedAccessTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private StaffMemberService staffMemberService;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private WappiService wappiService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private String adminToken;
    private String managerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        objectMapper = new ObjectMapper();
        
        // Создаем тестовых пользователей
        User adminUser = createTestUser("admin@test.com", UserRole.ADMIN);
        User managerUser = createTestUser("manager@test.com", UserRole.MANAGER);
        User employeeUser = createTestUser("employee@test.com", UserRole.EMPLOYEE);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(managerUser));
        when(userRepository.findByEmail("employee@test.com")).thenReturn(Optional.of(employeeUser));

        // Генерируем токены
        adminToken = jwtUtil.generateToken(adminUser, "test-tenant", "test-staff");
        managerToken = jwtUtil.generateToken(managerUser, "test-tenant", "test-staff");
        employeeToken = jwtUtil.generateToken(employeeUser, "test-tenant", "test-staff");

        // Stub CompanyRepository
        Company testCompany = new Company();
        testCompany.setId("test");
        testCompany.setName("Test Company");
        testCompany.setOwnerEmail("admin@test.com");
        when(companyRepository.findById("test")).thenReturn(Optional.of(testCompany));

        // Stub StaffMemberService
        StaffMember staffMember = new StaffMember();
        staffMember.setId("test-staff");
        staffMember.setName("Test Staff");
        staffMember.setTenantId("test-tenant");
        staffMember.setActive(true);
        when(staffMemberService.getStaffByIdAndDate(any(), any(), any())).thenReturn(Optional.of(staffMember));
        when(staffMemberService.getStaffPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 50), 0));

        // Stub StaffMemberRepository
        when(staffMemberRepository.findById("test-staff")).thenReturn(Optional.of(staffMember));

        // Stub WappiService
        WappiSettings wappiSettings = new WappiSettings();
        wappiSettings.setId("wappi-id");
        wappiSettings.setTenantId("test-tenant");
        wappiSettings.setEnabled(true);
        when(wappiService.getSettings(any())).thenReturn(wappiSettings);

        // Stub DashboardService
        when(dashboardService.getAdminStats(any())).thenReturn(java.util.Map.of("totalClients", 0));
    }

    private User createTestUser(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setTenantId("test-tenant");
        user.setStaffId("test-staff");
        return user;
    }

    // ========== ADMIN ROLE TESTS ==========
    
    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/staff")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/api/manager/workload")
                .param("year", "2024")
                .param("month", "5")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessEmployeeEndpoints() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ========== MANAGER ROLE TESTS ==========

    @Test
    void managerCanAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/api/manager/workload")
                .param("year", "2024")
                .param("month", "5")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/settings/wappi")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotAccessEmployeeEndpoints() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard/stats")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    // ========== EMPLOYEE ROLE TESTS ==========

    @Test
    void employeeCanAccessEmployeeEndpoints() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard/stats")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/employee/profile")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/api/manager/workload")
                .param("year", "2024")
                .param("month", "5")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    // ========== UNAUTHORIZED ACCESS TESTS ==========

    @Test
    void unauthorizedUserCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/workload"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/employee/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    // ========== PUBLIC ACCESS TESTS ==========

    @Test
    void publicEndpointsAreAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/companies/test"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/api/admin/dashboard/stats"))
                .andExpect(status().isOk());
    }

    // ========== CROSS-ROLE ACCESS VALIDATION ==========

    @Test
    void crossRoleAccessValidation() throws Exception {
        // Admin пытается получить доступ к employee эндпоинтам с неправильными параметрами
        mockMvc.perform(get("/api/employee/appointments")
                .param("date", "invalid-date")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Manager пытается изменить настройки wappi с пустым телом запроса
        mockMvc.perform(put("/api/manager/settings/wappi")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }
}
