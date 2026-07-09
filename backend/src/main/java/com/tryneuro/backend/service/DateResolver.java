package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Branch;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DateResolver {

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern THROUGH_N_DAYS = Pattern.compile("^(?:через|in)\\s+(\\d+)\\s+(?:дн|days?|дней|дня|day)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DAY_MONTH = Pattern.compile("^(\\d{1,2})[-_ ](.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Map<String, DayOfWeek> WEEKDAYS = Map.ofEntries(
            Map.entry("monday", DayOfWeek.MONDAY), Map.entry("mon", DayOfWeek.MONDAY),
            Map.entry("понедельник", DayOfWeek.MONDAY), Map.entry("пн", DayOfWeek.MONDAY),
            Map.entry("tuesday", DayOfWeek.TUESDAY), Map.entry("tue", DayOfWeek.TUESDAY), Map.entry("tu", DayOfWeek.TUESDAY),
            Map.entry("вторник", DayOfWeek.TUESDAY), Map.entry("вт", DayOfWeek.TUESDAY),
            Map.entry("wednesday", DayOfWeek.WEDNESDAY), Map.entry("wed", DayOfWeek.WEDNESDAY),
            Map.entry("среда", DayOfWeek.WEDNESDAY), Map.entry("среду", DayOfWeek.WEDNESDAY), Map.entry("ср", DayOfWeek.WEDNESDAY),
            Map.entry("thursday", DayOfWeek.THURSDAY), Map.entry("thu", DayOfWeek.THURSDAY), Map.entry("th", DayOfWeek.THURSDAY),
            Map.entry("четверг", DayOfWeek.THURSDAY), Map.entry("чт", DayOfWeek.THURSDAY),
            Map.entry("friday", DayOfWeek.FRIDAY), Map.entry("fri", DayOfWeek.FRIDAY),
            Map.entry("пятница", DayOfWeek.FRIDAY), Map.entry("пятницу", DayOfWeek.FRIDAY), Map.entry("пт", DayOfWeek.FRIDAY),
            Map.entry("saturday", DayOfWeek.SATURDAY), Map.entry("sat", DayOfWeek.SATURDAY),
            Map.entry("суббота", DayOfWeek.SATURDAY), Map.entry("субботу", DayOfWeek.SATURDAY), Map.entry("сб", DayOfWeek.SATURDAY),
            Map.entry("sunday", DayOfWeek.SUNDAY), Map.entry("sun", DayOfWeek.SUNDAY),
            Map.entry("воскресенье", DayOfWeek.SUNDAY), Map.entry("вс", DayOfWeek.SUNDAY)
    );

    private static final Map<String, Integer> MONTHS_BY_NAME = Map.ofEntries(
            Map.entry("января", 1), Map.entry("январь", 1),
            Map.entry("февраля", 2), Map.entry("февраль", 2),
            Map.entry("марта", 3), Map.entry("март", 3),
            Map.entry("апреля", 4), Map.entry("апрель", 4),
            Map.entry("мая", 5), Map.entry("май", 5),
            Map.entry("июня", 6), Map.entry("июнь", 6),
            Map.entry("июля", 7), Map.entry("июль", 7),
            Map.entry("августа", 8), Map.entry("август", 8),
            Map.entry("сентября", 9), Map.entry("сентябрь", 9),
            Map.entry("октября", 10), Map.entry("октябрь", 10),
            Map.entry("ноября", 11), Map.entry("ноябрь", 11),
            Map.entry("декабря", 12), Map.entry("декабрь", 12),
            Map.entry("january", 1), Map.entry("jan", 1),
            Map.entry("february", 2), Map.entry("feb", 2),
            Map.entry("march", 3), Map.entry("mar", 3),
            Map.entry("april", 4), Map.entry("apr", 4),
            Map.entry("june", 6), Map.entry("jun", 6),
            Map.entry("july", 7), Map.entry("jul", 7),
            Map.entry("august", 8), Map.entry("aug", 8),
            Map.entry("september", 9), Map.entry("sep", 9), Map.entry("sept", 9),
            Map.entry("october", 10), Map.entry("oct", 10),
            Map.entry("november", 11), Map.entry("nov", 11),
            Map.entry("december", 12), Map.entry("dec", 12)
    );

    public LocalDate resolve(Branch branch, String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("date is empty");
        }
        String s = dateStr.trim().toLowerCase(Locale.ROOT).replaceAll("[_\\s]+", " ");
        ZoneId zone = zoneOf(branch);
        LocalDate today = ZonedDateTime.now(zone).toLocalDate();

        if (ISO_DATE.matcher(dateStr).matches()) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid ISO date: " + dateStr);
            }
        }

        switch (s) {
            case "today", "сегодня", "сег" -> { return today; }
            case "tomorrow", "завтра", "завт" -> { return today.plusDays(1); }
            case "послезавтра", "day after tomorrow", "послезавт" -> { return today.plusDays(2); }
            case "next week", "на следующей неделе", "наследующей неделе", "наслед неделе" -> {
                return today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
            default -> {}
        }

        Matcher m = THROUGH_N_DAYS.matcher(s);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return today.plusDays(n);
        }

        boolean next = s.startsWith("next ") || s.startsWith("следующий ") || s.startsWith("следующая ") || s.startsWith("след ") || s.startsWith("следующ ");
        String weekdayKey = next ? s.replaceFirst("^(?:next|следующий|следующая|след|следующ)\\s+", "") : s;
        weekdayKey = weekdayKey.replaceAll("[_\\s]", "");
        DayOfWeek dow = WEEKDAYS.get(weekdayKey);
        if (dow == null) {
            dow = WEEKDAYS.get(s.replaceAll("[_\\s]", ""));
        }
        if (dow != null) {
            if (next) {
                return today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(dow));
            }
            LocalDate onOrNext = today.with(TemporalAdjusters.nextOrSame(dow));
            if (onOrNext.isEqual(today)) {
                return today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(dow));
            }
            return onOrNext;
        }

        Matcher dm = DAY_MONTH.matcher(s);
        if (dm.matches()) {
            try {
                int day = Integer.parseInt(dm.group(1));
                String monthName = dm.group(2).toLowerCase(Locale.ROOT);
                Integer month = MONTHS_BY_NAME.get(monthName);
                if (month == null) {
                    throw new IllegalArgumentException("Unknown month name: " + monthName);
                }
                int year = today.getYear();
                LocalDate candidate = LocalDate.of(year, month, day);
                if (candidate.isBefore(today)) {
                    candidate = LocalDate.of(year + 1, month, day);
                }
                return candidate;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid day in day-month: " + s);
            }
        }

        throw new IllegalArgumentException("Unknown date format: " + dateStr
                + ". Supported: YYYY-MM-DD, today, tomorrow, послезавтра, monday..sunday / пн..вс, next_monday, на_следующей_неделе, через_N_дней, 15_июля.");
    }

    private ZoneId zoneOf(Branch branch) {
        if (branch == null || branch.getTimezone() == null || branch.getTimezone().isBlank()) {
            throw new IllegalArgumentException("Branch timezone is not configured. All operations must use the branch timezone.");
        }
        try {
            return ZoneId.of(branch.getTimezone());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid branch timezone: " + branch.getTimezone());
        }
    }
}