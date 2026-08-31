package com.icsfilter.model;

import java.time.LocalDate;

/**
 * Chooses the earliest date from which table entries are shown. Each mode maps
 * to a concrete cutoff computed relative to a given reference day.
 */
public enum StartFrom {

    YEAR("Ab diesem Jahr"),
    MONTH("Ab diesem Monat"),
    TODAY("Ab heute");

    private final String label;

    StartFrom(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The first (inclusive) day shown for the given reference day. */
    public LocalDate effectiveDate(LocalDate today) {
        LocalDate d = today == null ? LocalDate.now() : today;
        return switch (this) {
            case YEAR -> d.withDayOfYear(1);
            case MONTH -> d.withDayOfMonth(1);
            case TODAY -> d;
        };
    }
}
