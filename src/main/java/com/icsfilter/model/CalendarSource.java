package com.icsfilter.model;

/**
 * A single source of calendar data: a human readable name and an ICS URL.
 */
public final class CalendarSource {

    private final String name;
    private final String url;

    public CalendarSource(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }
}
