package com.tryneuro.backend.service;

import com.tryneuro.backend.client.NotificationClient;
import com.tryneuro.backend.dto.ReturnReminderCandidate;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReturnReminderServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private ReturnReminderService returnReminderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(returnReminderService, "internalSecret", "test-secret");
    }

    @Test
    @DisplayName("getCandidates — маппинг Object[] в DTO")
    void testGetCandidates() {
        Timestamp lastVisit = Timestamp.valueOf(LocalDateTime.now().minusDays(45));
        Object[] row = {"contact-1", "Иван Петров", "+79991234567", "Замена масла", lastVisit};
        List<Object[]> rows = java.util.Collections.singletonList(row);
        when(appointmentRepository.findReturnReminderCandidates(eq("t-1"), any(OffsetDateTime.class)))
                .thenReturn(rows);

        List<ReturnReminderCandidate> result = returnReminderService.getCandidates("t-1", 30);

        assertEquals(1, result.size());
        assertEquals("contact-1", result.get(0).getContactId());
        assertEquals("Иван Петров", result.get(0).getName());
        assertEquals("+79991234567", result.get(0).getPhone());
        assertEquals("Замена масла", result.get(0).getLastService());
        assertEquals(45, result.get(0).getDaysSinceLastVisit());
    }

    @Test
    @DisplayName("getCandidates — пустой список если все были недавно")
    void testGetCandidatesEmpty() {
        when(appointmentRepository.findReturnReminderCandidates(anyString(), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        assertTrue(returnReminderService.getCandidates("t-1", 30).isEmpty());
    }

    @Test
    @DisplayName("getCount — возвращает правильное число")
    void testGetCount() {
        when(appointmentRepository.countReturnReminderCandidates(anyString(), any(OffsetDateTime.class)))
                .thenReturn(5L);
        assertEquals(5L, returnReminderService.getCount("t-1", 30));
    }

    @Test
    @DisplayName("sendReminder — успешная отправка")
    void testSendReminderSuccess() {
        Contact c = new Contact();
        c.setId("c-1"); c.setName("Иван"); c.setPhones(List.of("+79991234567"));
        when(contactRepository.findById("c-1")).thenReturn(Optional.of(c));

        Map<String, Object> r = returnReminderService.sendReminder("c-1", "Приходите!", "t-1");

        assertTrue((boolean) r.get("success"));
        verify(notificationClient).sendTelegramMessage(anyString(), anyMap());
    }

    @Test
    @DisplayName("sendReminder — контакт не найден")
    void testSendReminderNotFound() {
        when(contactRepository.findById("unknown")).thenReturn(Optional.empty());
        Map<String, Object> r = returnReminderService.sendReminder("unknown", "msg", "t-1");
        assertFalse((boolean) r.get("success"));
        assertEquals("Клиент не найден", r.get("error"));
        verify(notificationClient, never()).sendTelegramMessage(anyString(), anyMap());
    }

    @Test
    @DisplayName("sendReminder — нет телефона")
    void testSendReminderNoPhone() {
        Contact c = new Contact();
        c.setId("c-1"); c.setName("Иван"); c.setPhones(List.of());
        when(contactRepository.findById("c-1")).thenReturn(Optional.of(c));

        Map<String, Object> r = returnReminderService.sendReminder("c-1", "msg", "t-1");

        assertFalse((boolean) r.get("success"));
        assertEquals("У клиента нет номера телефона", r.get("error"));
    }

    @Test
    @DisplayName("sendReminder — 8 превращается в 7")
    void testPhoneNormalization() {
        Contact c = new Contact();
        c.setId("c-1"); c.setName("Иван"); c.setPhones(List.of("8 (999) 123-45-67"));
        when(contactRepository.findById("c-1")).thenReturn(Optional.of(c));

        returnReminderService.sendReminder("c-1", "msg", "t-1");

        verify(notificationClient).sendTelegramMessage(anyString(), argThat(m ->
                "79991234567".equals(m.get("phone"))
        ));
    }

    @Test
    @DisplayName("sendReminder — ошибка микросервиса")
    void testSendReminderError() {
        Contact c = new Contact();
        c.setId("c-1"); c.setName("Иван"); c.setPhones(List.of("+79991234567"));
        when(contactRepository.findById("c-1")).thenReturn(Optional.of(c));
        doThrow(new RuntimeException("timeout"))
                .when(notificationClient).sendTelegramMessage(anyString(), anyMap());

        Map<String, Object> r = returnReminderService.sendReminder("c-1", "msg", "t-1");

        assertFalse((boolean) r.get("success"));
        assertNotNull(r.get("error"));
    }
}
