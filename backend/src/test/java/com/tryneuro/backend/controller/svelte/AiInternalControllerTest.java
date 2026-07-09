package com.tryneuro.backend.controller.svelte;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.model.Service;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.repository.StaffMemberRepository;
import com.tryneuro.backend.service.AppServiceService;
import com.tryneuro.backend.service.BranchService;
import com.tryneuro.backend.service.ScheduleService;
import com.tryneuro.backend.service.StaffMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class AiInternalControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BranchService branchService;
    @MockBean private StaffMemberService staffMemberService;
    @MockBean private StaffMemberRepository staffMemberRepository;
    @MockBean private AppServiceService appServiceService;
    @MockBean private ScheduleService scheduleService;
    @MockBean private com.tryneuro.backend.repository.BranchRepository branchRepository;

    private MockMvc mockMvc;
    private final String testSecret = "test-secret-key";
    private final String tenantId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Branch branch(String id, String name) {
        Branch b = new Branch();
        b.setId(id);
        b.setName(name);
        b.setAddress("addr-" + id);
        b.setTimezone("Europe/Moscow");
        b.setTenantId(tenantId);
        b.setActive(true);
        return b;
    }

    @Test
    @DisplayName("GET /branches?query=фильтрует по имени (contains)")
    void getBranchesWithQueryFiltersByName() throws Exception {
        when(branchService.getBranches(tenantId))
            .thenReturn(List.of(branch("b1", "Виртуальный"), branch("b2", "Центр")));

        mockMvc.perform(get("/api/admin/ai/internal/branches")
                .param("query", "вирту")
                .header("X-Internal-Secret", testSecret)
                .header("X-Tenant-Id", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.branches.length()").value(1))
            .andExpect(jsonPath("$.branches[0].id").value("b1"))
            .andExpect(jsonPath("$.branches[0].name").value("Виртуальный"));
    }

    @Test
    @DisplayName("GET /branches без query возвращает все филиалы")
    void getBranchesWithoutQueryReturnsAll() throws Exception {
        when(branchService.getBranches(tenantId))
            .thenReturn(List.of(branch("b1", "A"), branch("b2", "B")));

        mockMvc.perform(get("/api/admin/ai/internal/branches")
                .header("X-Internal-Secret", testSecret)
                .header("X-Tenant-Id", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.branches.length()").value(2));
    }

    @Test
    @DisplayName("POST /staff/search с branchId фильтрует по филиалу")
    void searchStaffFiltersByBranchId() throws Exception {
        StaffMember s = new StaffMember();
        s.setId("s1");
        s.setName("Маша");
        s.setTenantId(tenantId);
        s.setActive(true);
        when(staffMemberRepository.findByTenantIdAndBranchIdWithBranches(tenantId, "b1"))
            .thenReturn(List.of(s));

        String body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId, "query", "", "branchId", "b1"));

        mockMvc.perform(post("/api/admin/ai/internal/staff/search")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("s1"));
    }

    @Test
    @DisplayName("POST /staff/search с пустым query возвращает всех (не 400)")
    void searchStaffEmptyQueryReturnsAll() throws Exception {
        StaffMember s = new StaffMember();
        s.setId("s1");
        s.setName("Маша");
        s.setTenantId(tenantId);
        s.setActive(true);
        when(staffMemberService.getStaffPaged(eq(tenantId), eq(""), eq(true), anyInt(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 50), 1));

        String body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId, "query", ""));

        mockMvc.perform(post("/api/admin/ai/internal/staff/search")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("s1"));
    }

    @Test
    @DisplayName("POST /services/search с пустым query возвращает все услуги (не 400)")
    void searchServicesEmptyQueryReturnsAll() throws Exception {
        Service svc = new Service();
        svc.setId("svc1");
        svc.setName("Стрижка");
        svc.setTenantId(tenantId);
        when(appServiceService.getAllServices(tenantId)).thenReturn(List.of(svc));

        String body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId, "query", ""));

        mockMvc.perform(post("/api/admin/ai/internal/services/search")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("svc1"));
    }

    @Test
    @DisplayName("POST /availability/slots возвращает массив слотов")
    void availabilitySlotsReturnsSlotsList() throws Exception {
        when(scheduleService.getAvailableSlotsForBranch(eq(tenantId), eq("s1"), org.mockito.ArgumentMatchers.isNull(), any(LocalDate.class), eq(60)))
            .thenReturn(List.of(
                Map.of("startTime", "09:00", "endTime", "10:00"),
                Map.of("startTime", "10:00", "endTime", "11:00")
            ));

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "staffId", "s1", "date", "2026-07-10", "duration", 60));

        mockMvc.perform(post("/api/admin/ai/internal/availability/slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slots.length()").value(2))
            .andExpect(jsonPath("$.slots[0].startTime").value("09:00"));
    }

    @Test
    @DisplayName("POST /availability/slots без staffId возвращает 400")
    void availabilitySlotsRejectMissingStaffId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "date", "2026-07-10", "duration", 60));

        mockMvc.perform(post("/api/admin/ai/internal/availability/slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /appointments со staffId вызывает getStaffMemberById (точный поиск)")
    void createAppointmentWithStaffIdUsesExactMatch() throws Exception {
        Service svc = new Service();
        svc.setId("svc1");
        svc.setName("Стрижка");
        svc.setDurationInMinutes(30);
        svc.setTenantId(tenantId);

        StaffMember staff = new StaffMember();
        staff.setId("s1");
        staff.setName("Маша");
        staff.setTenantId(tenantId);
        staff.setActive(true);

        when(staffMemberService.getStaffMemberById("s1")).thenReturn(Optional.of(staff));
        when(appServiceService.getAllServices(tenantId)).thenReturn(List.of(svc));
        when(scheduleService.addAppointment(any(), any(), anyBoolean()))
            .thenThrow(new RuntimeException("simulated"));

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "clientName", "Иван", "serviceName", "Стрижка",
            "dateTime", "2026-07-10T14:00:00+03:00", "staffId", "s1", "branchId", "b1"));

        mockMvc.perform(post("/api/admin/ai/internal/appointments")
                .header("X-Internal-Secret", testSecret)
                .header("X-Actor-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        verify(staffMemberService).getStaffMemberById("s1");
    }

    @Test
    @DisplayName("POST /availability/branch-slots возвращает мастеров со слотами (abs date)")
    void branchSlotsReturnsStaffWithSlots() throws Exception {
        com.tryneuro.backend.model.Branch branch = branch("b1", "Виртуальный");
        when(branchRepository.findById("b1")).thenReturn(Optional.of(branch));
        StaffMember s = new StaffMember();
        s.setId("s1");
        s.setName("Маша");
        s.setTenantId(tenantId);
        s.setActive(true);
        when(staffMemberRepository.findByTenantIdAndBranchIdWithBranches(tenantId, "b1"))
            .thenReturn(List.of(s));
        when(scheduleService.getAvailableSlotsForBranch(eq(tenantId), eq("s1"), eq("b1"), any(LocalDate.class), eq(60)))
            .thenReturn(List.of(Map.of("startTime", "09:00", "endTime", "10:00")));

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "branchId", "b1", "date", "2026-07-10", "duration", 60));

        mockMvc.perform(post("/api/admin/ai/internal/availability/branch-slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.branchId").value("b1"))
            .andExpect(jsonPath("$.timezone").value("Europe/Moscow"))
            .andExpect(jsonPath("$.staff[0].staffId").value("s1"))
            .andExpect(jsonPath("$.staff[0].staffName").value("Маша"))
            .andExpect(jsonPath("$.staff[0].slots[0].startTime").value("09:00"));
    }

    @Test
    @DisplayName("POST /availability/branch-slots с relative date 'tomorrow' резолвится через tz филиала")
    void branchSlotsRelativeDateResolved() throws Exception {
        com.tryneuro.backend.model.Branch branch = branch("b1", "Виртуальный");
        when(branchRepository.findById("b1")).thenReturn(Optional.of(branch));
        when(staffMemberRepository.findByTenantIdAndBranchIdWithBranches(tenantId, "b1"))
            .thenReturn(List.of());
        when(scheduleService.getAvailableSlotsForBranch(eq(tenantId), anyString(), eq("b1"), any(LocalDate.class), eq(60)))
            .thenReturn(List.of());

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "branchId", "b1", "date", "tomorrow"));

        mockMvc.perform(post("/api/admin/ai/internal/availability/branch-slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").exists());
    }

    @Test
    @DisplayName("POST /availability/branch-slots без branchId возвращает 400")
    void branchSlotsRejectMissingBranchId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "date", "2026-07-10"));

        mockMvc.perform(post("/api/admin/ai/internal/availability/branch-slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /availability/branch-slots с несуществующим branchId возвращает 404")
    void branchSlotsUnknownBranchReturns404() throws Exception {
        when(branchRepository.findById("unknown")).thenReturn(Optional.empty());

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "branchId", "unknown", "date", "2026-07-10"));

        mockMvc.perform(post("/api/admin/ai/internal/availability/branch-slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /availability/slots с relative date и без branchId возвращает 400")
    void slotsRelativeDateWithoutBranchIdReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "staffId", "s1", "date", "tomorrow"));

        mockMvc.perform(post("/api/admin/ai/internal/availability/slots")
                .header("X-Internal-Secret", testSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /appointments с branchId+date+time собирает OffsetDateTime в tz филиала")
    void createAppointmentWithBranchDateAndTime() throws Exception {
        com.tryneuro.backend.model.Branch branch = branch("b1", "Виртуальный");
        Service svc = new Service();
        svc.setId("svc1");
        svc.setName("Стрижка");
        svc.setDurationInMinutes(30);
        svc.setTenantId(tenantId);

        when(branchRepository.findById("b1")).thenReturn(Optional.of(branch));
        when(appServiceService.getAllServices(tenantId)).thenReturn(List.of(svc));
        when(scheduleService.addAppointment(any(), any(), anyBoolean()))
            .thenThrow(new RuntimeException("simulated"));

        String body = objectMapper.writeValueAsString(Map.of(
            "tenantId", tenantId, "clientName", "Иван", "serviceName", "Стрижка",
            "branchId", "b1", "date", "2026-07-10", "time", "14:00"));

        mockMvc.perform(post("/api/admin/ai/internal/appointments")
                .header("X-Internal-Secret", testSecret)
                .header("X-Actor-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        verify(branchRepository).findById("b1");
    }

    @Test
    @DisplayName("GET /branches с разными timezone возвращает ambiguous=true")
    void getBranchesWithDifferentTimezonesIsAmbiguous() throws Exception {
        com.tryneuro.backend.model.Branch b1 = branch("b1", "Москва");
        com.tryneuro.backend.model.Branch b2 = branch("b2", "Владивосток");
        b2.setTimezone("Asia/Vladivostok");
        when(branchService.getBranches(tenantId)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/admin/ai/internal/branches")
                .header("X-Internal-Secret", testSecret)
                .header("X-Tenant-Id", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ambiguous").value(true))
            .andExpect(jsonPath("$.timezones.length()").value(2));
    }
}