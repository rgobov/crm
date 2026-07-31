package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционный тест RENT span-запросов (H2, тестовый профиль).
 * Проверяет, что многодневная аренда видна на каждом покрытом дне,
 * а конфликт ресурса определяется по всему интервалу.
 */
@SpringBootTest
@ActiveProfiles("test")
class AppointmentRepositoryRentSpanTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private ResourceRepository resourceRepository;

    private Branch createdBranch;
    private Resource createdResource;

    @BeforeEach
    void setUp() {
        createdBranch = new Branch();
        createdBranch.setName("Тест-филиал аренды");
        createdBranch.setTimezone("Europe/Moscow");
        createdBranch.setNiche("RENT");
        createdBranch.setTenantId("span-tenant");
        createdBranch = branchRepository.save(createdBranch);

        createdResource = new Resource();
        createdResource.setName("Бокс 1");
        createdResource.setBranchId(createdBranch.getId());
        createdResource.setTenantId(createdBranch.getTenantId());
        createdResource = resourceRepository.save(createdResource);
    }

    private Appointment save(String id, String tenant, String branch, String resource,
                             OffsetDateTime start, int duration, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setTenantId(tenant);
        a.setBranchId(branch);
        a.setResourceId(resource);
        a.setStartTime(start);
        a.setDurationInMinutes(duration);
        a.setService("Аренда");
        a.setClientName("Client");
        a.setClientPhone("123");
        a.setStatus(status);
        return appointmentRepository.save(a);
    }

    @Test
    @DisplayName("findSpanningDay возвращает многодневную аренду на каждом покрытом дне")
    void findSpanningDay_returnsMultiDayOnEveryCoveredDay() {
        String tenant = createdBranch.getTenantId();
        String branch = createdBranch.getId();
        String res = createdResource.getId();

        // Многодневная аренда: 2026-02-21T09:00+03:00 на 48 часов (покрывает 21 и 22 число)
        Appointment sp1 = save("sp-1", tenant, branch, res, OffsetDateTime.parse("2026-02-21T09:00:00+03:00"), 2880, AppointmentStatus.SCHEDULED);
        // Обычная запись 23 числа
        Appointment sp2 = save("sp-2", tenant, branch, res, OffsetDateTime.parse("2026-02-23T10:00:00+03:00"), 120, AppointmentStatus.SCHEDULED);

        // День 21 по Москве: [2026-02-20T21:00Z, 2026-02-21T21:00Z)
        List<Appointment> day21 = appointmentRepository.findSpanningDay(tenant, branch,
                OffsetDateTime.parse("2026-02-20T21:00:00Z"), OffsetDateTime.parse("2026-02-21T21:00:00Z"));
        assertEquals(1, day21.size());
        assertEquals(sp1.getId(), day21.get(0).getId());

        // День 22 по Москве: аренда всё ещё продолжается
        List<Appointment> day22 = appointmentRepository.findSpanningDay(tenant, branch,
                OffsetDateTime.parse("2026-02-21T21:00:00Z"), OffsetDateTime.parse("2026-02-22T21:00:00Z"));
        assertEquals(1, day22.size());
        assertEquals(sp1.getId(), day22.get(0).getId());

        // День 23 по Москве: sp-1 ещё заканчивается утром (09:00+03) + sp-2
        List<Appointment> day23 = appointmentRepository.findSpanningDay(tenant, branch,
                OffsetDateTime.parse("2026-02-22T21:00:00Z"), OffsetDateTime.parse("2026-02-23T21:00:00Z"));
        assertEquals(2, day23.size());
        assertTrue(day23.stream().anyMatch(a -> a.getId().equals(sp1.getId())));
        assertTrue(day23.stream().anyMatch(a -> a.getId().equals(sp2.getId())));
    }

    @Test
    @DisplayName("findResourceSpan находит конфликт по всему интервалу и игнорирует CANCELLED")
    void findResourceSpan_detectsOverlapAcrossDaysAndIgnoresCancelled() {
        String tenant = createdBranch.getTenantId();
        String branch = createdBranch.getId();
        String res = createdResource.getId();

        // Существующая аренда 21-23 февраля
        Appointment rs1 = save("rs-1", tenant, branch, res, OffsetDateTime.parse("2026-02-21T09:00:00+03:00"), 2880, AppointmentStatus.SCHEDULED);
        // Отменённая запись в тот же период — не должна конфликтовать
        save("rs-2", tenant, branch, res, OffsetDateTime.parse("2026-02-22T10:00:00+03:00"), 120, AppointmentStatus.CANCELLED);

        // Новая аренда с пересечением на второй день — конфликт
        List<Appointment> overlap = appointmentRepository.findResourceSpan(tenant, res,
                OffsetDateTime.parse("2026-02-22T10:00:00+03:00"), OffsetDateTime.parse("2026-02-22T12:00:00+03:00"));
        assertEquals(1, overlap.size());
        assertEquals(rs1.getId(), overlap.get(0).getId());

        // Запись после окончания аренды — свободно
        List<Appointment> free = appointmentRepository.findResourceSpan(tenant, res,
                OffsetDateTime.parse("2026-02-23T11:00:00+03:00"), OffsetDateTime.parse("2026-02-23T12:00:00+03:00"));
        assertTrue(free.isEmpty());
    }
}
