package com.icsfilter.model;

/**
 * A single source of calendar data: a human readable name, an ICS URL and an
 * optional filter string. A non-blank filter restricts the shown events to
 * those whose summary/title contains the filter (case-insensitive).
 */
public final class CalendarSource {

    private final String name;
    private final String url;
    private final String filter;

    public CalendarSource(String name, String url) {
        this(name, url, "");
    }

    public CalendarSource(String name, String url, String filter) {
        this.name = name;
        this.url = url;
        this.filter = filter == null ? "" : filter;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    public String filter() {
        return filter;
    }

    /** Returns the filter lower-cased, or an empty string when there is none. */
    public String filterLower() {
        return filter.toLowerCase();
    }

    @Override
    public String toString() {
        return name;
    }
}
