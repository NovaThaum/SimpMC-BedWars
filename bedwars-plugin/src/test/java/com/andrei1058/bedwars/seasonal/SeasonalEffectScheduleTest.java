package com.andrei1058.bedwars.seasonal;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonalEffectScheduleTest {

    @Test
    void newYearWindowCrossesCalendarYearAndUsesExclusiveEnd() {
        SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse("12-31 23:55", "01-01 00:15");
        assertFalse(schedule.isActive(ZonedDateTime.parse("2026-12-31T23:54:59+08:00[Asia/Shanghai]")));
        assertTrue(schedule.isActive(ZonedDateTime.parse("2026-12-31T23:55:00+08:00[Asia/Shanghai]")));
        assertTrue(schedule.isActive(ZonedDateTime.parse("2027-01-01T00:14:59+08:00[Asia/Shanghai]")));
        assertFalse(schedule.isActive(ZonedDateTime.parse("2027-01-01T00:15:00+08:00[Asia/Shanghai]")));
    }

    @Test
    void sameYearWindowIsBoundedAndInvalidInputIsRejected() {
        SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse("10-22 00:00", "11-02 00:00");

        assertFalse(schedule.isActive(ZonedDateTime.parse("2026-10-21T23:59:59Z")));
        assertTrue(schedule.isActive(ZonedDateTime.parse("2026-10-22T00:00:00Z")));
        assertFalse(schedule.isActive(ZonedDateTime.parse("2026-11-02T00:00:00Z")));
        assertThrows(IllegalArgumentException.class,
                () -> SeasonalEffectSchedule.parse("02-30 00:00", "03-01 00:00"));
    }
}
