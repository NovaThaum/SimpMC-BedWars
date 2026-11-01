package com.andrei1058.bedwars.seasonal;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

final class SeasonalEffectSchedule {
    private final MonthDay startDate;
    private final int startHour;
    private final int startMinute;
    private final MonthDay endDate;
    private final int endHour;
    private final int endMinute;

    private SeasonalEffectSchedule(MonthDay startDate, int startHour, int startMinute,
                                   MonthDay endDate, int endHour, int endMinute) {
        this.startDate = startDate;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endDate = endDate;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    static SeasonalEffectSchedule parse(String start, String end) {
        try {
            LocalDateTime from = LocalDateTime.parse("2000-" + start.replace(' ', 'T'));
            LocalDateTime to = LocalDateTime.parse("2000-" + end.replace(' ', 'T'));
            return new SeasonalEffectSchedule(MonthDay.from(from), from.getHour(), from.getMinute(),
                    MonthDay.from(to), to.getHour(), to.getMinute());
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("时间必须使用 MM-dd HH:mm 格式", exception);
        }
    }

    boolean isActiveNow() {
        return isActive(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    boolean isActive(ZonedDateTime now) {
        int year = now.getYear();
        LocalDateTime start = date(startDate, year).atTime(startHour, startMinute);
        LocalDateTime end = date(endDate, year).atTime(endHour, endMinute);
        LocalDateTime current = now.toLocalDateTime();
        if (endDate.compareTo(startDate) <= 0) {
            if (current.isBefore(start)) start = start.minusYears(1);
            else end = end.plusYears(1);
        }
        return !current.isBefore(start) && current.isBefore(end);
    }

    private static LocalDate date(MonthDay day, int year) {
        try {
            return day.atYear(year);
        } catch (DateTimeException ignored) {
            return day.atYear(year - 1);
        }
    }
}
