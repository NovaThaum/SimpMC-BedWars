package com.andrei1058.bedwars.seasonal;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public record SeasonalEffectSchedule(MonthDay startDate, int startHour, int startMinute,
                                    MonthDay endDate, int endHour, int endMinute) {

    public static SeasonalEffectSchedule parse(String start, String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse("2000-" + start.replace(' ', 'T'));
            LocalDateTime endTime = LocalDateTime.parse("2000-" + end.replace(' ', 'T'));
            return new SeasonalEffectSchedule(
                    MonthDay.of(startTime.getMonth(), startTime.getDayOfMonth()), startTime.getHour(), startTime.getMinute(),
                    MonthDay.of(endTime.getMonth(), endTime.getDayOfMonth()), endTime.getHour(), endTime.getMinute());
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("时间窗口必须使用 MM-dd HH:mm 格式", exception);
        }
    }

    public boolean isActive(ZonedDateTime now) {
        int year = now.getYear();
        LocalDateTime start = atYear(startDate, year).atTime(startHour, startMinute);
        LocalDateTime end = atYear(endDate, year).atTime(endHour, endMinute);
        LocalDateTime current = now.toLocalDateTime();

        if (endDate.compareTo(startDate) <= 0) {
            if (current.isBefore(start)) {
                start = start.minusYears(1);
            } else {
                end = end.plusYears(1);
            }
        }
        return !current.isBefore(start) && current.isBefore(end);
    }

    public boolean isActiveNow() {
        return isActive(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    private static java.time.LocalDate atYear(MonthDay monthDay, int year) {
        try {
            return monthDay.atYear(year);
        } catch (java.time.DateTimeException ignored) {
            return monthDay.atYear(year - 1);
        }
    }
}
