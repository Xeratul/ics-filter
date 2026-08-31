package com.icsfilter.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A single calendar event parsed from an ICS feed.
 */
public final class CalendarEvent {

    private final String uid;
    private final String summary;
    private final String description;
    private final String location;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final boolean allDay;
    private final String category;
    private final CalendarSource source;

    public CalendarEvent(String uid, String summary, String description, String location,
                         ZonedDateTime start, ZonedDateTime end, boolean allDay,
                         String category, CalendarSource source) {
        this.uid = uid;
        this.summary = summary == null ? "" : summary;
        this.description = description == null ? "" : description;
        this.location = location == null ? "" : location;
        this.start = start;
        this.end = end;
        this.allDay = allDay;
        this.category = category == null ? "" : category;
        this.source = source;
    }

    public String uid() { return uid; }

    public String summary() { return summary; }

    public String description() { return description; }

    public String location() { return location; }

    public ZonedDateTime start() { return start; }

    public ZonedDateTime end() { return end; }

    public boolean allDay() { return allDay; }

    public String category() { return category; }

    public CalendarSource source() { return source; }

    /** The calendar day on which this event starts (system zone). */
    public LocalDate startDate() {
        return start == null ? null : start.toLocalDate();
    }

    /**
     * The last calendar day (inclusive) on which this event appears (system zone).
     * <p>
     * All-day events use an exclusive {@code DTEND} in iCalendar, so an event ending
     * on day {@code X} actually occupies day {@code X-1} as its final day. Timed
     * events also skip their nominal end day when they end exactly at midnight.
     */
    public LocalDate lastDay() {
        if (start == null) {
            return null;
        }
        if (end == null) {
            return start.toLocalDate();
        }
        LocalDate endDay = end.toLocalDate();
        boolean coversEndDay = !end.isEqual(endDay.atStartOfDay(ZoneId.systemDefault()));
        LocalDate last = coversEndDay ? endDay : endDay.minusDays(1);
        LocalDate first = start.toLocalDate();
        return last.isBefore(first) ? first : last;
    }

    public boolean isMultiDay() {
        LocalDate first = startDate();
        LocalDate last = lastDay();
        return first != null && last != null && last.isAfter(first);
    }
}
