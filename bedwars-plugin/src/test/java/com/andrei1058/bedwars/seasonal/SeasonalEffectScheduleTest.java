package com.andrei1058.bedwars.seasonal;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeasonalEffectScheduleTest {

    @Test
    public void newYearWindowIncludesStartAndExcludesEndAcrossYearBoundary() {
        SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse("12-31 23:55", "01-01 00:15");
        assertFalse(schedule.isActive(at(2026, 12, 31, 23, 54)));
        assertTrue(schedule.isActive(at(2026, 12, 31, 23, 55)));
        assertTrue(schedule.isActive(at(2027, 1, 1, 0, 14)));
        assertFalse(schedule.isActive(at(2027, 1, 1, 0, 15)));
    }

    @Test
    public void halloweenWindowUsesConfiguredDateAndEndBoundary() {
        SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse("10-22 00:00", "11-02 00:00");
        assertFalse(schedule.isActive(at(2026, 10, 21, 23, 59)));
        assertTrue(schedule.isActive(at(2026, 10, 22, 0, 0)));
        assertTrue(schedule.isActive(at(2026, 11, 1, 23, 59)));
        assertFalse(schedule.isActive(at(2026, 11, 2, 0, 0)));
    }

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("UTC"));
    }
}
