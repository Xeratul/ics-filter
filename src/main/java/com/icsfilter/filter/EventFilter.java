package com.icsfilter.filter;

import com.icsfilter.model.CalendarEvent;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of criteria used to decide whether an event is shown. An empty set
 * means "all" (no restriction).
 */
public final class EventFilter {

    private String keyword = "";
    private LocalDate from;
    private LocalDate to;
    private final Set<String> categories = new HashSet<>();
    private final Set<String> sources = new HashSet<>();

    public String keyword() { return keyword; }

    public void keyword(String keyword) { this.keyword = keyword == null ? "" : keyword; }

    public LocalDate from() { return from; }

    public void from(LocalDate from) { this.from = from; }

    public LocalDate to() { return to; }

    public void to(LocalDate to) { this.to = to; }

    public Set<String> categories() { return categories; }

    public Set<String> sources() { return sources; }

    public void clear() {
        keyword = "";
        from = null;
        to = null;
        categories.clear();
        sources.clear();
    }

    public boolean matches(CalendarEvent event) {
        if (!keyword.isBlank()) {
            String k = keyword.toLowerCase();
            if (!event.summary().toLowerCase().contains(k)
                    && !event.description().toLowerCase().contains(k)
                    && !event.location().toLowerCase().contains(k)) {
                return false;
            }
        }
        LocalDate start = event.startDate();
        if (start != null) {
            if (from != null && start.isBefore(from)) {
                return false;
            }
            if (to != null && start.isAfter(to)) {
                return false;
            }
        }
        if (!categories.isEmpty() && !categories.contains(event.category())) {
            return false;
        }
        if (!sources.isEmpty() && !sources.contains(event.source().name())) {
            return false;
        }
        return true;
    }

    public List<CalendarEvent> apply(List<CalendarEvent> events) {
        return events.stream().filter(this::matches).toList();
    }
}
