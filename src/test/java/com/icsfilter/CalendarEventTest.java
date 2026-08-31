package com.icsfilter;

import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarEventTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final CalendarSource SOURCE = new CalendarSource("Test", "https://example.com/test.ics");

    private static CalendarEvent event(ZonedDateTime start, ZonedDateTime end, boolean allDay) {
        return new CalendarEvent("uid", "Summary", "", "", start, end, allDay, "Cat", SOURCE);
    }

    private static ZonedDateTime at(int y, int m, int d, int hour, int min) {
        return ZonedDateTime.of(y, m, d, hour, min, 0, 0, ZONE);
    }

    @Test
    void allDayEventEndDateIsExclusive() {
        // DTSTART=2026-05-01 DTEND=2026-05-04 -> occupies 01, 02, 03.
        CalendarEvent e = event(at(2026, 5, 1, 0, 0), at(2026, 5, 4, 0, 0), true);
        assertEquals(LocalDate.of(2026, 5, 1), e.startDate());
        assertEquals(LocalDate.of(2026, 5, 3), e.lastDay());
        assertTrue(e.isMultiDay());
    }

    @Test
    void singleAllDayEventWithExclusiveEndIsNotMultiDay() {
        // DTSTART=2026-05-01 DTEND=2026-05-02 -> only day 01.
        CalendarEvent e = event(at(2026, 5, 1, 0, 0), at(2026, 5, 2, 0, 0), true);
        assertEquals(LocalDate.of(2026, 5, 1), e.lastDay());
        assertFalse(e.isMultiDay());
    }

    @Test
    void timedEventCoversUntilItsEndDay() {
        CalendarEvent e = event(at(2026, 5, 1, 10, 0), at(2026, 5, 3, 18, 0), false);
        assertEquals(LocalDate.of(2026, 5, 3), e.lastDay());
        assertTrue(e.isMultiDay());
    }

    @Test
    void timedEventEndingAtMidnightSkipsEndDay() {
        CalendarEvent e = event(at(2026, 5, 1, 10, 0), at(2026, 5, 4, 0, 0), false);
        assertEquals(LocalDate.of(2026, 5, 3), e.lastDay());
        assertTrue(e.isMultiDay());
    }

    @Test
    void singleDayEventWithoutEndCoversOnlyStartDay() {
        CalendarEvent e = event(at(2026, 5, 1, 9, 0), null, false);
        assertEquals(LocalDate.of(2026, 5, 1), e.lastDay());
        assertFalse(e.isMultiDay());
    }

    @Test
    void singleDayTimedEventCoversOnlyStartDay() {
        CalendarEvent e = event(at(2026, 5, 1, 9, 0), at(2026, 5, 1, 10, 0), false);
        assertEquals(LocalDate.of(2026, 5, 1), e.lastDay());
        assertFalse(e.isMultiDay());
    }
}
