package com.tryneuro.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentTimeTest {

    @Test
    @DisplayName("При отсутствии филиала используется часовой пояс Москвы (UTC+3)")
    void testMoscowDefaultTimezone() {
        Appointment app = new Appointment();
        // 10:00 UTC = 13:00 Moscow
        app.setStartTime(OffsetDateTime.of(2026, 2, 21, 10, 0, 0, 0, ZoneOffset.UTC));
        app.setBranch(null);

        assertEquals(LocalTime.of(13, 0), app.getTime(), "Должно быть 13:00 по Москве");
        assertEquals(LocalDate.of(2026, 2, 21), app.getDate());
    }

    @Test
    @DisplayName("Корректный пересчет времени для разных часовых поясов")
    void testDifferentTimezones() {
        Appointment app = new Appointment();
        // 10:00 UTC
        app.setStartTime(OffsetDateTime.of(2026, 2, 21, 10, 0, 0, 0, ZoneOffset.UTC));

        // Кейс 1: Владивосток (UTC+10)
        Branch vlk = new Branch();
        vlk.setTimezone("Asia/Vladivostok");
        app.setBranch(vlk);
        assertEquals(LocalTime.of(20, 0), app.getTime(), "10:00 UTC во Владивостоке должно быть 20:00");

        // Кейс 2: Калининград (UTC+2)
        Branch kgd = new Branch();
        kgd.setTimezone("Europe/Kaliningrad");
        app.setBranch(kgd);
        assertEquals(LocalTime.of(12, 0), app.getTime(), "10:00 UTC в Калининграде должно быть 12:00");
    }

    @Test
    @DisplayName("Переход даты при большой разнице часовых поясов")
    void testDateChangeOnTimezone() {
        Appointment app = new Appointment();
        // 22:00 UTC 21 февраля
        app.setStartTime(OffsetDateTime.of(2026, 2, 21, 22, 0, 0, 0, ZoneOffset.UTC));

        // Москва (UTC+3) -> Это уже 01:00 ночи 22 февраля
        Branch msk = new Branch();
        msk.setTimezone("Europe/Moscow");
        app.setBranch(msk);

        assertEquals(LocalTime.of(1, 0), app.getTime());
        assertEquals(LocalDate.of(2026, 2, 22), app.getDate(), "В Москве дата должна уже смениться на 22 февраля");
    }
}
