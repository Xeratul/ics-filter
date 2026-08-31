package com.icsfilter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A single source of calendar data: a human readable name, an ICS URL, an
 * optional filter string, an optional color (as a CSS hex string such as
 * "#e63946") and a list of event titles that should be hidden. A non-blank
 * filter restricts the shown events to those whose summary/title contains the
 * filter (case-insensitive). A title in {@link #ignoreTitles()} hides every
 * event whose summary equals it exactly (case-sensitive). A blank color means
 * the default palette color for the source's index is used.
 */
public final class CalendarSource {

    private final String name;
    private final String url;
    private final String filter;
    private final String color;
    private final List<String> ignoreTitles;

    public CalendarSource(String name, String url) {
        this(name, url, "", "", List.of());
    }

    public CalendarSource(String name, String url, String filter) {
        this(name, url, filter, "", List.of());
    }

    public CalendarSource(String name, String url, String filter, String color) {
        this(name, url, filter, color, List.of());
    }

    public CalendarSource(String name, String url, String filter, String color, List<String> ignoreTitles) {
        this.name = name;
        this.url = url;
        this.filter = filter == null ? "" : filter;
        this.color = color == null ? "" : color;
        List<String> cleaned = ignoreTitles == null ? new ArrayList<>() : new ArrayList<>(ignoreTitles);
        cleaned.removeIf(t -> t == null || t.isBlank());
        this.ignoreTitles = List.copyOf(cleaned);
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

    /** The titles whose events are hidden for this source (exact match). */
    public List<String> ignoreTitles() {
        return ignoreTitles;
    }

    /** Returns true when the given summary should be hidden for this source. */
    public boolean ignores(String summary) {
        if (summary == null) {
            return false;
        }
        return ignoreTitles.contains(summary);
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
