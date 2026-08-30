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
                       String keyword, LocalDate from, LocalDate to, Set<String> categories) {
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
            if (name.isEmpty() && url.isEmpty()) {
                continue;
            }
            sources.add(new CalendarSource(name, url, filter));
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
        Set<String> categories = new LinkedHashSet<>();
        String catProp = p.getProperty("filter.categories", "");
        if (!catProp.isBlank()) {
            categories.addAll(List.of(catProp.split(",")));
        }
        return new Data(sources, enabled, keyword, from, to, categories);
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
            }

            List<String> enabled = new ArrayList<>(data.enabled());
            p.setProperty("enabled.count", String.valueOf(enabled.size()));
            for (int i = 0; i < enabled.size(); i++) {
                p.setProperty("enabled." + i, enabled.get(i));
            }

            p.setProperty("filter.keyword", data.keyword() == null ? "" : data.keyword());
            p.setProperty("filter.from", data.from() == null ? "" : data.from().toString());
            p.setProperty("filter.to", data.to() == null ? "" : data.to().toString());
            p.setProperty("filter.categories", String.join(",", data.categories()));

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
