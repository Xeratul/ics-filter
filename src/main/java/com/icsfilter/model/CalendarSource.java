package com.icsfilter.model;

/**
 * A single source of calendar data: a human readable name, an ICS URL, an
 * optional filter string and an optional color (as a CSS hex string such as
 * "#e63946"). A non-blank filter restricts the shown events to those whose
 * summary/title contains the filter (case-insensitive). A blank color means the
 * default palette color for the source's index is used.
 */
public final class CalendarSource {

    private final String name;
    private final String url;
    private final String filter;
    private final String color;

    public CalendarSource(String name, String url) {
        this(name, url, "");
    }

    public CalendarSource(String name, String url, String filter) {
        this(name, url, filter, "");
    }

    public CalendarSource(String name, String url, String filter, String color) {
        this.name = name;
        this.url = url;
        this.filter = filter == null ? "" : filter;
        this.color = color == null ? "" : color;
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

    /** Returns the configured color as a CSS hex string, or an empty string. */
    public String color() {
        return color;
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
