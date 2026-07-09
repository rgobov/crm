package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Branch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

class DateResolverTest {

    private final DateResolver resolver = new DateResolver();

    private Branch branch(String tz) {
        Branch b = new Branch();
        b.setId("b1");
        b.setName("Test");
        b.setTimezone(tz);
        b.setTenantId("t1");
        return b;
    }

    private LocalDate today(String tz) {
        return ZonedDateTime.now(ZoneId.of(tz)).toLocalDate();
    }

    @Test
    @DisplayName("ISO дата парсится как есть")
    void isoDateParsed() {
        assertEquals(LocalDate.of(2026, 7, 10), resolver.resolve(branch("Europe/Moscow"), "2026-07-10"));
    }

    @Test
    @DisplayName("today/сегодня возвращает текущую дату в tz филиала")
    void todayReturnsCurrentDate() {
        assertEquals(today("Europe/Moscow"), resolver.resolve(branch("Europe/Moscow"), "today"));
        assertEquals(today("Europe/Moscow"), resolver.resolve(branch("Europe/Moscow"), "сегодня"));
        assertEquals(today("Asia/Vladivostok"), resolver.resolve(branch("Asia/Vladivostok"), "today"));
    }

    @Test
    @DisplayName("tomorrow/завтра = сегодня+1 в tz филиала")
    void tomorrowReturnsNextDay() {
        assertEquals(today("Europe/Moscow").plusDays(1), resolver.resolve(branch("Europe/Moscow"), "tomorrow"));
        assertEquals(today("Europe/Moscow").plusDays(1), resolver.resolve(branch("Europe/Moscow"), "завтра"));
        assertEquals(today("Asia/Vladivostok").plusDays(1), resolver.resolve(branch("Asia/Vladivostok"), "завтра"));
    }

    @Test
    @DisplayName("послезавтра = сегодня+2")
    void dayAfterTomorrow() {
        assertEquals(today("Europe/Moscow").plusDays(2), resolver.resolve(branch("Europe/Moscow"), "послезавтра"));
        assertEquals(today("Europe/Moscow").plusDays(2), resolver.resolve(branch("Europe/Moscow"), "day_after_tomorrow"));
    }

    @Test
    @DisplayName("через_N_дней прибавляет N дней")
    void throughNDays() {
        assertEquals(today("Europe/Moscow").plusDays(3), resolver.resolve(branch("Europe/Moscow"), "через_3_дней"));
        assertEquals(today("Europe/Moscow").plusDays(5), resolver.resolve(branch("Europe/Moscow"), "через 5 дней"));
        assertEquals(today("Europe/Moscow").plusDays(7), resolver.resolve(branch("Europe/Moscow"), "in 7 days"));
    }

    @Test
    @DisplayName("понедельник/monday возвращает ближайший понедельник (сегодня или след.)")
    void weekdayNearest() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "понедельник");
        LocalDate today = today("Europe/Moscow");
        LocalDate expected = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (expected.isEqual(today)) {
            expected = today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("next_friday/следующий_пт = пятница след. недели")
    void nextWeekday() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "next_friday");
        LocalDate resultRu = resolver.resolve(branch("Europe/Moscow"), "следующий_пт");
        LocalDate today = today("Europe/Moscow");
        LocalDate expected = today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        assertEquals(expected, result);
        assertEquals(expected, resultRu);
    }

    @Test
    @DisplayName("на_следующей_неделе = понедельник след. недели")
    void nextWeek() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "на_следующей_неделе");
        LocalDate resultEn = resolver.resolve(branch("Europe/Moscow"), "next_week");
        LocalDate today = today("Europe/Moscow");
        LocalDate expected = today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertEquals(expected, result);
        assertEquals(expected, resultEn);
    }

    @Test
    @DisplayName("15_июля резолвится в ближайшее 15 июля (этот или след. год)")
    void dayMonth() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "15_июля");
        LocalDate today = today("Europe/Moscow");
        LocalDate candidate = LocalDate.of(today.getYear(), 7, 15);
        if (candidate.isBefore(today)) {
            candidate = LocalDate.of(today.getYear() + 1, 7, 15);
        }
        assertEquals(candidate, result);
        assertEquals(candidate, resolver.resolve(branch("Europe/Moscow"), "15 july"));
    }

    @Test
    @DisplayName("Разные tz дают разные даты для today (на границе суток)")
    void differentTzDifferentDate() {
        LocalDate moscowToday = resolver.resolve(branch("Europe/Moscow"), "today");
        LocalDate vladToday = resolver.resolve(branch("Asia/Vladivostok"), "today");
        assertNotNull(moscowToday);
        assertNotNull(vladToday);
    }

    @Test
    @DisplayName("Неизвестный формат бросает IllegalArgumentException")
    void unknownFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(branch("Europe/Moscow"), "непонятнаядата"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(branch("Europe/Moscow"), "2026/07/10"));
    }

    @Test
    @DisplayName("Пустая/null дата бросает")
    void emptyDateThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(branch("Europe/Moscow"), ""));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(branch("Europe/Moscow"), null));
    }

    @Test
    @DisplayName("Филиал без timezone бросает (нет fallback на Москву)")
    void branchWithoutTimezoneThrows() {
        Branch b = branch(null);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(b, "tomorrow"));
        Branch b2 = branch("");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(b2, "tomorrow"));
    }

    @Test
    @DisplayName("Невалидный tz бросает")
    void invalidTimezoneThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(branch("Not/A/Real/Zone"), "tomorrow"));
    }

    @Test
    @DisplayName("Сокращения пн/вт/ср работают")
    void shortWeekdayNames() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "пт");
        LocalDate today = today("Europe/Moscow");
        LocalDate expected = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        if (expected.isEqual(today)) {
            expected = today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        }
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("mon/sat/tue английские сокращения работают")
    void englishShortWeekdays() {
        LocalDate result = resolver.resolve(branch("Europe/Moscow"), "mon");
        LocalDate today = today("Europe/Moscow");
        LocalDate expected = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (expected.isEqual(today)) {
            expected = today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }
        assertEquals(expected, result);
    }
}