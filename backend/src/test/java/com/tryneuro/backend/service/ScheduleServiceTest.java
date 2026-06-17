package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.BranchRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private StaffShiftRepository staffShiftRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ContactService contactService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Мастер доступен внутри обычной дневной смены")
    void testStaffAvailable_DayShift() {
        String staffId = "staff-1";
        String branchId = "branch-1";
        LocalDate date = LocalDate.of(2026, 2, 21);
        LocalTime time = LocalTime.of(10, 0); // 10:00 утра
        int duration = 60;

        StaffShift shift = new StaffShift();
        shift.setWorkStartTime(LocalTime.of(9, 0));
        shift.setWorkEndTime(LocalTime.of(18, 0));
        shift.setDayOff(false);

        when(staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId))
                .thenReturn(Optional.of(shift));
        when(appointmentRepository.findByTenantIdAndStaffMemberIdAndDate("t1", staffId, date))
                .thenReturn(new ArrayList<>());

        boolean result = scheduleService.isStaffMemberAvailable("t1", staffId, date, time, duration, null, branchId);
        assertTrue(result, "Мастер должен быть доступен в 10:00 при смене 09-18");
    }

    @Test
    @DisplayName("Мастер доступен в ночную смену (переход через полночь)")
    void testStaffAvailable_NightShift() {
        String staffId = "staff-night";
        String branchId = "branch-1";
        LocalDate date = LocalDate.of(2026, 2, 21);
        LocalTime time = LocalTime.of(21, 0); // 21:00 вечера
        int duration = 60; // До 22:00

        StaffShift shift = new StaffShift();
        shift.setWorkStartTime(LocalTime.of(13, 0));
        shift.setWorkEndTime(LocalTime.of(1, 0)); // До часу ночи следующего дня
        shift.setDayOff(false);

        when(staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId))
                .thenReturn(Optional.of(shift));
        when(appointmentRepository.findByTenantIdAndStaffMemberIdAndDate("t1", staffId, date))
                .thenReturn(new ArrayList<>());

        boolean result = scheduleService.isStaffMemberAvailable("t1", staffId, date, time, duration, null, branchId);
        assertTrue(result, "Мастер должен быть доступен в 21:00 при смене 13:00 - 01:00");
    }

    @Test
    @DisplayName("Запись отклоняется, если выходит за границы смены")
    void testStaffNotAvailable_OutsideShift() {
        String staffId = "staff-1";
        String branchId = "branch-1";
        LocalDate date = LocalDate.of(2026, 2, 21);
        LocalTime time = LocalTime.of(8, 0); // 8:00 утра (смена с 9:00)
        int duration = 30;

        StaffShift shift = new StaffShift();
        shift.setWorkStartTime(LocalTime.of(9, 0));
        shift.setWorkEndTime(LocalTime.of(18, 0));

        when(staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId))
                .thenReturn(Optional.of(shift));

        boolean result = scheduleService.isStaffMemberAvailable("t1", staffId, date, time, duration, null, branchId);
        assertFalse(result, "Мастер не должен быть доступен до начала смены");
    }

    @Test
    @DisplayName("Создание групповой записи генерирует groupId и сохраняет несколько записей")
    void testAddAppointment_GroupCreation() {
        String branchId = "branch-1";
        LocalDate date = LocalDate.of(2026, 2, 21);
        LocalTime time = LocalTime.of(10, 0);
        int duration = 60;

        com.tryneuro.backend.model.Branch branch = new com.tryneuro.backend.model.Branch();
        branch.setId(branchId);
        branch.setTimezone("UTC");
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        StaffShift shift = new StaffShift();
        shift.setWorkStartTime(LocalTime.of(9, 0));
        shift.setWorkEndTime(LocalTime.of(18, 0));
        shift.setDayOff(false);

        when(staffShiftRepository.findByStaffIdAndDateAndBranchId("staff-1", date, branchId))
                .thenReturn(Optional.of(shift));
        when(staffShiftRepository.findByStaffIdAndDateAndBranchId("staff-2", date, branchId))
                .thenReturn(Optional.of(shift));

        Appointment app = new Appointment();
        app.setTenantId("t1");
        app.setBranchId(branchId);
        app.setStartTime(java.time.OffsetDateTime.of(date, time, java.time.ZoneOffset.UTC));
        app.setDurationInMinutes(duration);
        app.setService("Test Service");
        app.setClientName("Client");
        app.setClientPhone("123");

        java.util.List<String> staffIds = java.util.List.of("staff-1", "staff-2");

        when(appointmentRepository.save(org.mockito.Mockito.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = scheduleService.addAppointment(app, staffIds, false);

        org.junit.jupiter.api.Assertions.assertNotNull(result.getGroupId());
        org.mockito.Mockito.verify(appointmentRepository, org.mockito.Mockito.times(2))
                .save(org.mockito.Mockito.any(Appointment.class));
    }

    @Test
    @DisplayName("Обход валидации занятости с флагом force=true")
    void testAddAppointment_ForceFlagOnConflict() {
        String branchId = "branch-1";
        LocalDate date = LocalDate.of(2026, 2, 21);
        LocalTime time = LocalTime.of(10, 0);

        com.tryneuro.backend.model.Branch branch = new com.tryneuro.backend.model.Branch();
        branch.setId(branchId);
        branch.setTimezone("UTC");
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        // Смена не найдена (будет расценено как нерабочий день/конфликт)
        when(staffShiftRepository.findByStaffIdAndDateAndBranchId("staff-1", date, branchId))
                .thenReturn(Optional.empty());

        Appointment app = new Appointment();
        app.setTenantId("t1");
        app.setBranchId(branchId);
        app.setStartTime(java.time.OffsetDateTime.of(date, time, java.time.ZoneOffset.UTC));
        app.setDurationInMinutes(60);
        app.setService("Test Service");
        app.setClientName("Client");
        app.setClientPhone("123");

        // Без force=true должно выбросить исключение ResponseStatusException
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            scheduleService.addAppointment(app, java.util.List.of("staff-1"), false);
        });

        // С force=true должно пройти успешно
        when(appointmentRepository.save(org.mockito.Mockito.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = scheduleService.addAppointment(app, java.util.List.of("staff-1"), true);
        org.junit.jupiter.api.Assertions.assertNotNull(result);
    }
}
