package com.icsfilter;

import com.icsfilter.ical.Recurrence;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecurrenceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private static List<LocalDate> dates(List<ZonedDateTime> occ) {
        return occ.stream().map(ZonedDateTime::toLocalDate).collect(Collectors.toList());
    }

    private static ZonedDateTime date(int y, int m, int d, int h) {
        return ZonedDateTime.of(y, m, d, h, 0, 0, 0, ZONE);
    }

    @Test
    void dailyIntervalWithCount() {
        Recurrence rec = Recurrence.parse("FREQ=DAILY;INTERVAL=2;COUNT=5");
        ZonedDateTime dtStart = date(2024, 1, 1, 9);
        List<ZonedDateTime> occ = rec.occurrences(dtStart, date(2024, 1, 1, 0), date(2024, 2, 1, 0));
        assertEquals(List.of(
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3),
                        LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 7),
                        LocalDate.of(2024, 1, 9)),
                dates(occ));
    }

    @Test
    void weeklyByMoWeWithCount() {
        Recurrence rec = Recurrence.parse("FREQ=WEEKLY;BYDAY=MO,WE;COUNT=6");
        ZonedDateTime dtStart = date(2024, 1, 1, 10);
        List<ZonedDateTime> occ = rec.occurrences(dtStart, date(2024, 1, 1, 0), date(2024, 2, 1, 0));
        assertEquals(List.of(
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3),
                        LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 17)),
                dates(occ));
    }

    @Test
    void monthlyOrdinalSecondSunday() {
        Recurrence rec = Recurrence.parse("FREQ=MONTHLY;BYDAY=2SU;COUNT=4");
        ZonedDateTime dtStart = date(2024, 1, 14, 9);
        List<ZonedDateTime> occ = rec.occurrences(dtStart, date(2024, 1, 1, 0), date(2024, 12, 1, 0));
        assertEquals(List.of(
                        LocalDate.of(2024, 1, 14), LocalDate.of(2024, 2, 11),
                        LocalDate.of(2024, 3, 10), LocalDate.of(2024, 4, 14)),
                dates(occ));
    }

    @Test
    void monthlyByMonthDay() {
        Recurrence rec = Recurrence.parse("FREQ=MONTHLY;BYMONTHDAY=15;COUNT=3");
        ZonedDateTime dtStart = date(2024, 1, 15, 8);
        List<ZonedDateTime> occ = rec.occurrences(dtStart, date(2024, 1, 1, 0), date(2024, 12, 1, 0));
        assertEquals(List.of(
                        LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15),
                        LocalDate.of(2024, 3, 15)),
                dates(occ));
    }

    @Test
    void weeklyUntilDateInclusive() {
        Recurrence rec = Recurrence.parse("FREQ=WEEKLY;BYDAY=MO;UNTIL=20240115T000000Z");
        ZonedDateTime dtStart = date(2024, 1, 1, 9);
        List<ZonedDateTime> occ = rec.occurrences(dtStart, date(2024, 1, 1, 0), date(2024, 2, 1, 0));
        assertEquals(List.of(
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8),
                        LocalDate.of(2024, 1, 15)),
                dates(occ));
    }
}
