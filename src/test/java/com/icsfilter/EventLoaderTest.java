package com.icsfilter;

import com.icsfilter.ical.EventLoader;
import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventLoaderTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final String ICS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//test//ics-filter//EN
            BEGIN:VEVENT
            UID:single-1
            DTSTAMP:20240101T000000Z
            DTSTART:20240102T090000Z
            DTEND:20240102T100000Z
            SUMMARY:Ein Termin
            LOCATION:Büro
            CATEGORIES:Meetings
            END:VEVENT
            BEGIN:VEVENT
            UID:rec-1
            DTSTAMP:20240101T000000Z
            DTSTART:20240103T100000Z
            DTEND:20240103T110000Z
            RRULE:FREQ=DAILY;COUNT=3
            SUMMARY:Wiederkehrend
            END:VEVENT
            END:VCALENDAR
            """;

    @Test
    void parsesSingleAndRecurringEvents() throws Exception {
        CalendarSource source = new CalendarSource("Test", "https://example.com/test.ics");
        EventLoader loader = new EventLoader();
        ZonedDateTime winStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZONE);
        ZonedDateTime winEnd = ZonedDateTime.of(2024, 12, 1, 0, 0, 0, 0, ZONE);

        List<CalendarEvent> events = loader.parse(ICS, source, winStart, winEnd);

        // 1 single event + 3 daily recurrences.
        assertEquals(4, events.size());
        assertEquals("Ein Termin", events.get(0).summary());
        assertEquals("Meetings", filterCategory(events));
    }

    private String filterCategory(List<CalendarEvent> events) {
        for (CalendarEvent e : events) {
            if (e.category() != null && !e.category().isBlank()) {
                return e.category();
            }
        }
        return "";
    }
}
