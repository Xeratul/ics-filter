package com.icsfilter.ical;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Minimal RFC 5545 recurrence expansion that covers the frequencies commonly
 * used by calendar feeds (DAILY, WEEKLY, MONTHLY, YEARLY) with INTERVAL,
 * COUNT, UNTIL, BYDAY, BYMONTH and BYMONTHDAY. It is self-contained so it can
 * be unit tested without depending on ical4j internals.
 */
public final class Recurrence {

    enum Freq { DAILY, WEEKLY, MONTHLY, YEARLY }

    private Freq freq;
    private int interval;
    private int count;              // 0 = unlimited
    private LocalDateTime until;    // null = unlimited (inclusive)
    private final Set<Integer> byMonth = new HashSet<>();
    private final Set<Integer> byMonthDay = new HashSet<>();
    private final List<DaySpec> byDay = new ArrayList<>();
    private boolean hasByMonth;
    private boolean hasByMonthDay;
    private boolean hasByDay;
    private boolean valid = true;

    private Recurrence(Freq freq, int interval, int count, LocalDateTime until) {
        this.freq = freq;
        this.interval = Math.max(interval, 1);
        this.count = count;
        this.until = until;
    }

    /**
     * A weekday selector, optionally a macro (nth or last) weekday such as
     * "2SU" (second Sunday) or "-1SA" (last Saturday).
     */
    private record DaySpec(int ordinal, DayOfWeek dayOfWeek) {
        static DaySpec parse(String token) {
            String t = token;
            int ordinal = 0;
            boolean neg = false;
            int idx = 0;
            while (idx < t.length() && (Character.isDigit(t.charAt(idx)) || t.charAt(idx) == '-')) {
                if (t.charAt(idx) == '-') {
                    neg = true;
                }
                idx++;
            }
            if (idx > 0) {
                String numStr = t.substring(0, idx).replace("-", "");
                if (!numStr.isEmpty()) {
                    ordinal = Integer.parseInt(numStr) * (neg ? -1 : 1);
                }
            }
            DayOfWeek dow = toDayOfWeek(t.substring(idx));
            return new DaySpec(ordinal, dow);
        }
    }

    private static DayOfWeek toDayOfWeek(String s) {
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    /** Parses an RRULE value (the part after "RRULE:") into a Recurrence. */
    public static Recurrence parse(String value) {
        Recurrence rec = new Recurrence(Freq.YEARLY, 1, 0, null);
        rec.valid = false;
        for (String part : value.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = part.substring(0, eq).toUpperCase(Locale.ROOT);
            String v = part.substring(eq + 1);
            switch (key) {
                case "FREQ" -> rec.freq = parseFreq(v);
                case "INTERVAL" -> rec.interval = Integer.parseInt(v);
                case "COUNT" -> rec.count = Integer.parseInt(v);
                case "UNTIL" -> rec.until = parseUntil(v);
                case "BYMONTH" -> {
                    for (String m : v.split(",")) {
                        rec.byMonth.add(Integer.parseInt(m));
                    }
                    rec.hasByMonth = true;
                }
                case "BYMONTHDAY" -> {
                    for (String d : v.split(",")) {
                        rec.byMonthDay.add(Integer.parseInt(d));
                    }
                    rec.hasByMonthDay = true;
                }
                case "BYDAY" -> {
                    for (String d : v.split(",")) {
                        DaySpec ds = DaySpec.parse(d);
                        if (ds.dayOfWeek() != null) {
                            rec.byDay.add(ds);
                        }
                    }
                    rec.hasByDay = true;
                }
                default -> { /* ignore unknown keys */ }
            }
        }
        rec.valid = true;
        return rec;
    }

    private static Freq parseFreq(String v) {
        return switch (v.toUpperCase(Locale.ROOT)) {
            case "DAILY" -> Freq.DAILY;
            case "WEEKLY" -> Freq.WEEKLY;
            case "MONTHLY" -> Freq.MONTHLY;
            case "YEARLY" -> Freq.YEARLY;
            default -> Freq.YEARLY;
        };
    }

    private static LocalDateTime parseUntil(String v) {
        DateTimeFormatter utc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyyMMdd");
        try {
            return LocalDateTime.parse(v, utc);
        } catch (RuntimeException e) {
            return LocalDate.parse(v, date).atStartOfDay();
        }
    }

    /**
     * Generates occurrence start times (in dtStart's zone) within
     * [windowStart, windowEnd], respecting COUNT and UNTIL.
     */
    public List<ZonedDateTime> occurrences(ZonedDateTime dtStart, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
        List<ZonedDateTime> result = new ArrayList<>();
        if (!valid || dtStart == null) {
            return result;
        }
        ZoneId zone = dtStart.getZone();
        LocalTime time = dtStart.toLocalTime();

        int generated = 0;
        // The first occurrence is always the DTSTART itself.
        LocalDate cursor = dtStart.toLocalDate();
        LocalDate firstWindowDate = windowStart.toLocalDate();
        LocalDate lastWindowDate = windowEnd.toLocalDate();

        // Iterate day-by-day (bounded) and test each day against the rule.
        LocalDate limit = lastWindowDate.plusDays(1);
        while (!cursor.isAfter(limit)) {
            if (isOccurrence(dtStart.toLocalDate(), cursor)) {
                generated++;
                LocalDateTime occ = LocalDateTime.of(cursor, time);
                ZonedDateTime z = occ.atZone(zone);
                if (!z.isBefore(windowStart) && !z.isAfter(windowEnd)) {
                    result.add(z);
                }
                if (count > 0 && generated >= count) {
                    break;
                }
            }
            if (until != null && cursor.isAfter(until.toLocalDate())) {
                break;
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private boolean isOccurrence(LocalDate seed, LocalDate candidate) {
        if (candidate.isBefore(seed)) {
            return false;
        }
        if (hasByMonth && !byMonth.contains(candidate.getMonthValue())) {
            return false;
        }
        return switch (freq) {
            case DAILY -> {
                long days = ChronoUnit.DAYS.between(seed, candidate);
                yield days % interval == 0 && matchesByMonthDay(candidate) && matchesByDay(candidate);
            }
            case WEEKLY -> {
                LocalDate intervalBoundary = seed.plusDays((seed.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7);
                long weeks = ChronoUnit.WEEKS.between(intervalBoundary, candidate
                        .minusDays((candidate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7));
                yield weeks >= 0 && weeks % interval == 0 && matchesByDay(candidate) && matchesByMonthDay(candidate);
            }
            case MONTHLY -> {
                long months = ChronoUnit.MONTHS.between(seed.withDayOfMonth(1), candidate.withDayOfMonth(1));
                if (months < 0 || months % interval != 0) {
                    yield false;
                }
                yield matchesMonthlyDay(seed, candidate);
            }
            case YEARLY -> {
                long years = candidate.getYear() - seed.getYear();
                if (years < 0 || years % interval != 0) {
                    yield false;
                }
                yield matchesMonthlyDay(seed, candidate);
            }
        };
    }

    private boolean matchesMonthlyDay(LocalDate seed, LocalDate candidate) {
        if (hasByDay && !byDay.isEmpty()) {
            for (DaySpec ds : byDay) {
                if (ds.ordinal() == 0) {
                    if (candidate.getDayOfWeek() == ds.dayOfWeek()) {
                        return true;
                    }
                } else if (matchesOrdinal(candidate, ds)) {
                    return true;
                }
            }
            return false;
        }
        if (hasByMonthDay) {
            return byMonthDay.contains(candidate.getDayOfMonth())
                    || byMonthDay.contains(candidate.getDayOfMonth() - (candidate.lengthOfMonth() + 1));
        }
        return candidate.getDayOfMonth() == seed.getDayOfMonth()
                || (hasByMonth && candidate.getDayOfMonth() == seed.getDayOfMonth());
    }

    private boolean matchesOrdinal(LocalDate candidate, DaySpec ds) {
        if (candidate.getDayOfWeek() != ds.dayOfWeek()) {
            return false;
        }
        int ord = ds.ordinal();
        if (ord > 0) {
            int occurrence = 1 + (candidate.getDayOfMonth() - 1) / 7;
            return occurrence == ord;
        } else {
            // ordinal < 0 counts from the end of the month (e.g. -1 = last).
            int occurrenceFromEnd = 1 + (candidate.lengthOfMonth() - candidate.getDayOfMonth()) / 7;
            return occurrenceFromEnd == -ord;
        }
    }

    private boolean matchesByMonthDay(LocalDate candidate) {
        if (!hasByMonthDay) {
            return true;
        }
        return byMonthDay.contains(candidate.getDayOfMonth())
                || byMonthDay.contains(candidate.getDayOfMonth() - (candidate.lengthOfMonth() + 1));
    }

    private boolean matchesByDay(LocalDate candidate) {
        if (!hasByDay || byDay.isEmpty()) {
            return true;
        }
        for (DaySpec ds : byDay) {
            if (ds.ordinal() == 0 && candidate.getDayOfWeek() == ds.dayOfWeek()) {
                return true;
            }
        }
        return false;
    }
}
