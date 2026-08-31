package com.icsfilter.store;

import com.icsfilter.model.CalendarSource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Loads and saves the persistent configuration (sources, enabled set and the
 * global filter) to a local properties file. Escaping is handled by
 * {@link Properties}.
 */
public final class ConfigStore {

    private final Path file;

    public ConfigStore() {
        this(Path.of(System.getProperty("user.home"), ".ics-filter", "config.properties"));
    }

    public ConfigStore(Path file) {
        this.file = file;
    }

    /** All persisted application state. */
    public record Data(List<CalendarSource> sources, Set<String> enabled,
                       String keyword, LocalDate from, LocalDate to, Set<String> categories,
                       String startFrom, Layout layout) {

        public Data(List<CalendarSource> sources, Set<String> enabled,
                    String keyword, LocalDate from, LocalDate to, Set<String> categories,
                    Layout layout) {
            this(sources, enabled, keyword, from, to, categories, "YEAR", layout);
        }

        public Data(List<CalendarSource> sources, Set<String> enabled,
                    String keyword, LocalDate from, LocalDate to, Set<String> categories) {
            this(sources, enabled, keyword, from, to, categories, "YEAR", new Layout());
        }
    }

    /** Persisted window layout: split dividers, window geometry and column widths. */
    public record Layout(double hDivider, double vDivider,
                         double windowX, double windowY, double windowWidth, double windowHeight,
                         List<Double> columnWidths) {

        public Layout() {
            this(0.7, 0.62, Double.NaN, Double.NaN, 1180, 740, List.of());
        }
    }

    public Data load() {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException e) {
                System.err.println("Config load failed: " + e.getMessage());
            }
        }

        List<CalendarSource> sources = new ArrayList<>();
        int count = intProp(p, "source.count", 0);
        for (int i = 0; i < count; i++) {
            String name = p.getProperty("source." + i + ".name", "");
            String url = p.getProperty("source." + i + ".url", "");
            String filter = p.getProperty("source." + i + ".filter", "");
            String color = p.getProperty("source." + i + ".color", "");
            if (name.isEmpty() && url.isEmpty()) {
                continue;
            }
            List<String> ignoreTitles = new ArrayList<>();
            int ignoreCount = intProp(p, "source." + i + ".ignore.count", 0);
            for (int j = 0; j < ignoreCount; j++) {
                String t = p.getProperty("source." + i + ".ignore." + j, "");
                if (!t.isBlank()) {
                    ignoreTitles.add(t);
                }
            }
            sources.add(new CalendarSource(name, url, filter, color, ignoreTitles));
        }

        Set<String> enabled = new LinkedHashSet<>();
        int enabledCount = intProp(p, "enabled.count", 0);
        for (int i = 0; i < enabledCount; i++) {
            String name = p.getProperty("enabled." + i, "");
            if (!name.isBlank()) {
                enabled.add(name);
            }
        }

        String keyword = p.getProperty("filter.keyword", "");
        LocalDate from = parseDate(p.getProperty("filter.from", ""));
        LocalDate to = parseDate(p.getProperty("filter.to", ""));
        String startFrom = p.getProperty("filter.startFrom", "YEAR");
        Set<String> categories = new LinkedHashSet<>();
        String catProp = p.getProperty("filter.categories", "");
        if (!catProp.isBlank()) {
            categories.addAll(List.of(catProp.split(",")));
        }

        double hDivider = doubleProp(p, "layout.hDivider", 0.7);
        double vDivider = doubleProp(p, "layout.vDivider", 0.62);
        double windowX = doubleProp(p, "layout.windowX", Double.NaN);
        double windowY = doubleProp(p, "layout.windowY", Double.NaN);
        double windowWidth = doubleProp(p, "layout.windowWidth", 1180);
        double windowHeight = doubleProp(p, "layout.windowHeight", 740);
        List<Double> columnWidths = new ArrayList<>();
        int colCount = intProp(p, "layout.columns.count", 0);
        for (int i = 0; i < colCount; i++) {
            columnWidths.add(doubleProp(p, "layout.columns." + i, 0.0));
        }
        Layout layout = new Layout(hDivider, vDivider, windowX, windowY,
                windowWidth, windowHeight, columnWidths);

        return new Data(sources, enabled, keyword, from, to, categories, startFrom, layout);
    }

    public void save(Data data) {
        try {
            Files.createDirectories(file.getParent());
            Properties p = new Properties();

            List<CalendarSource> sources = data.sources();
            p.setProperty("source.count", String.valueOf(sources.size()));
            for (int i = 0; i < sources.size(); i++) {
                CalendarSource s = sources.get(i);
                p.setProperty("source." + i + ".name", s.name());
                p.setProperty("source." + i + ".url", s.url());
                p.setProperty("source." + i + ".filter", s.filter());
                p.setProperty("source." + i + ".color", s.color());
                List<String> ignore = s.ignoreTitles();
                p.setProperty("source." + i + ".ignore.count", String.valueOf(ignore.size()));
                for (int j = 0; j < ignore.size(); j++) {
                    p.setProperty("source." + i + ".ignore." + j, ignore.get(j));
                }
            }

            List<String> enabled = new ArrayList<>(data.enabled());
            p.setProperty("enabled.count", String.valueOf(enabled.size()));
            for (int i = 0; i < enabled.size(); i++) {
                p.setProperty("enabled." + i, enabled.get(i));
            }

            p.setProperty("filter.keyword", data.keyword() == null ? "" : data.keyword());
            p.setProperty("filter.from", data.from() == null ? "" : data.from().toString());
            p.setProperty("filter.to", data.to() == null ? "" : data.to().toString());
            p.setProperty("filter.startFrom", data.startFrom() == null ? "YEAR" : data.startFrom());
            p.setProperty("filter.categories", String.join(",", data.categories()));

            Layout layout = data.layout();
            p.setProperty("layout.hDivider", String.valueOf(layout.hDivider()));
            p.setProperty("layout.vDivider", String.valueOf(layout.vDivider()));
            p.setProperty("layout.windowX", String.valueOf(layout.windowX()));
            p.setProperty("layout.windowY", String.valueOf(layout.windowY()));
            p.setProperty("layout.windowWidth", String.valueOf(layout.windowWidth()));
            p.setProperty("layout.windowHeight", String.valueOf(layout.windowHeight()));
            List<Double> widths = layout.columnWidths();
            p.setProperty("layout.columns.count", String.valueOf(widths.size()));
            for (int i = 0; i < widths.size(); i++) {
                p.setProperty("layout.columns." + i, String.valueOf(widths.get(i)));
            }

            try (Writer w = Files.newBufferedWriter(file)) {
                p.store(w, "ICS Filter configuration");
            }
        } catch (IOException e) {
            System.err.println("Config save failed: " + e.getMessage());
        }
    }

    private static int intProp(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double doubleProp(Properties p, String key, double fallback) {
        try {
            return Double.parseDouble(p.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
