package com.icsfilter.ical;

import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Downloads ICS feeds over HTTP and parses them into {@link CalendarEvent}s.
 */
public final class EventLoader {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final HttpClient http;

    public EventLoader() {
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeUrl(url)))
                .header("User-Agent", "ics-filter/1.0")
                .header("Accept", "text/calendar, text/plain, */*")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " for " + url);
        }
        return response.body();
    }

    /**
     * Replaces a leading {@code webcal://} (or {@code webcal:}) scheme with
     * {@code https:}, since {@link HttpRequest} cannot fetch the {@code webcal}
     * pseudo-scheme. Non-{@code webcal} URLs are returned unchanged.
     */
    public static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.regionMatches(true, 0, "webcal://", 0, 9)) {
            return "https://" + trimmed.substring(9);
        }
        if (trimmed.regionMatches(true, 0, "webcal:", 0, 7)) {
            return "https:" + trimmed.substring(7);
        }
        return url;
    }

    /** Downloads and parses a single source. Returns an empty list on failure. */
    public List<CalendarEvent> load(CalendarSource source, ZonedDateTime winStart, ZonedDateTime winEnd) {
        try {
            String ics = download(source.url());
            return parse(ics, source, winStart, winEnd);
        } catch (Exception e) {
            System.err.println("Failed to load " + source.name() + ": " + e.getMessage());
            return List.of();
        }
    }

    /** Loads all sources and considers non-fatal per-source failures. */
    public List<CalendarEvent> loadAll(List<CalendarSource> sources, ZonedDateTime winStart, ZonedDateTime winEnd) {
        List<CalendarEvent> all = new ArrayList<>();
        for (CalendarSource source : sources) {
            all.addAll(load(source, winStart, winEnd));
        }
        return all;
    }

    public List<CalendarEvent> parse(String ics, CalendarSource source, ZonedDateTime winStart, ZonedDateTime winEnd)
            throws IOException, ParserException {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(new ByteArrayInputStream(ics.getBytes(StandardCharsets.UTF_8)));

        List<CalendarEvent> result = new ArrayList<>();
        for (Component component : calendar.getComponents(Component.VEVENT)) {
            if (!(component instanceof VEvent event)) {
                continue;
            }
            result.addAll(parseEvent(event, source, winStart, winEnd));
        }
        return result;
    }

    private List<CalendarEvent> parseEvent(VEvent event, CalendarSource source,
                                            ZonedDateTime winStart, ZonedDateTime winEnd) {
        List<CalendarEvent> result = new ArrayList<>();
        Property dtStartProp = event.getProperty(Property.DTSTART);
        if (dtStartProp == null) {
            return result;
        }
        ParsedDate start = parseDateProperty(dtStartProp);
        if (start == null) {
            return result;
        }
        ParsedDate end = parseDateProperty(event.getProperty(Property.DTEND));

        String summary = text(event, Property.SUMMARY);
        String description = text(event, Property.DESCRIPTION);
        String location = text(event, Property.LOCATION);
        String uid = Objects.requireNonNullElse(text(event, Property.UID), "no-uid");
        String category = firstCategory(event);

        // Recurring multi-day events are not expanded: the day-by-day expansion
        // of a multi-day occurrence would produce overlapping/incomplete entries.
        boolean multiDay = end != null && !end.date.toLocalDate().equals(start.date.toLocalDate());

        Property rruleProp = event.getProperty(Property.RRULE);
        if (rruleProp != null && !multiDay) {
            try {
                Recurrence recurrence = Recurrence.parse(rruleProp.getValue());
                for (ZonedDateTime occ : recurrence.occurrences(start.date, winStart, winEnd)) {
                    ZonedDateTime occEnd = end != null ? occ.plus(Duration.between(start.date, end.date)) : null;
                    result.add(new CalendarEvent(uid, summary, description, location,
                            occ, occEnd, start.allDay, category, source));
                }
            } catch (RuntimeException ex) {
                result.add(new CalendarEvent(uid, summary, description, location,
                        start.date, end == null ? null : end.date, start.allDay, category, source));
            }
        } else {
            result.add(new CalendarEvent(uid, summary, description, location,
                    start.date, end == null ? null : end.date, start.allDay, category, source));
        }
        return result;
    }

    private static String text(VEvent event, String propertyName) {
        Property p = event.getProperty(propertyName);
        if (p == null) {
            return null;
        }
        String v = p.getValue();
        return v == null || v.isBlank() ? null : v;
    }

    private static String firstCategory(VEvent event) {
        Property p = event.getProperty(Property.CATEGORIES);
        if (p == null) {
            return null;
        }
        String v = p.getValue();
        if (v == null || v.isBlank()) {
            return null;
        }
        String first = v.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    /** Parsed date/time plus a flag for all-day (DATE value) events. */
    private record ParsedDate(ZonedDateTime date, boolean allDay) {
    }

    private ParsedDate parseDateProperty(Property property) {
        if (!(property instanceof DateProperty dateProp)) {
            return null;
        }
        String value = dateProp.getValue();
        if (value == null) {
            return null;
        }
        if (value.length() == 8) {
            LocalDate day = LocalDate.parse(value, FMT_DATE);
            return new ParsedDate(day.atStartOfDay(ZoneId.systemDefault()), true);
        }
        Instant instant = parseDateTime(value, resolveZone(dateProp));
        if (instant == null) {
            return null;
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return new ParsedDate(zdt, false);
    }

    private Instant parseDateTime(String value, ZoneId zone) {
        try {
            if (value.endsWith("Z") || value.endsWith("z")) {
                String stripped = value.substring(0, value.length() - 1);
                return LocalDateTime.parse(stripped, FMT_DATETIME).toInstant(ZoneOffset.UTC);
            }
            LocalDateTime ldt = LocalDateTime.parse(value, FMT_DATETIME);
            return ldt.atZone(zone).toInstant();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ZoneId resolveZone(DateProperty prop) {
        Parameter tzParam = prop.getParameter(Parameter.TZID);
        if (tzParam != null) {
            String tzid = tzParam.getValue();
            try {
                return ZoneId.of(tzid);
            } catch (RuntimeException ex) {
                return ZoneId.systemDefault();
            }
        }
        return ZoneId.systemDefault();
    }
}
