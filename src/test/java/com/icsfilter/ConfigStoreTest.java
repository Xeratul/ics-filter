package com.icsfilter;

import com.icsfilter.model.CalendarSource;
import com.icsfilter.store.ConfigStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigStoreTest {

    @Test
    void roundTripsSourcesFiltersAndEnabled(@TempDir Path dir) {
        Path file = dir.resolve("config.properties");
        ConfigStore store = new ConfigStore(file);

        List<CalendarSource> sources = List.of(
                new CalendarSource("Arbeit", "https://example.com/work.ics", "Meeting", "#457b9d"),
                new CalendarSource("Privat", "https://example.com/private.ics", ""));
        LinkedHashSet<String> enabled = new LinkedHashSet<>(List.of("Arbeit"));
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 2, 1);
        LinkedHashSet<String> categories = new LinkedHashSet<>(List.of("Urlaub", "Meeting"));

        store.save(new ConfigStore.Data(sources, enabled, "Urlaub", from, to, categories));

        ConfigStore.Data loaded = store.load();
        assertEquals(2, loaded.sources().size());
        assertEquals("Arbeit", loaded.sources().get(0).name());
        assertEquals("https://example.com/work.ics", loaded.sources().get(0).url());
        assertEquals("Meeting", loaded.sources().get(0).filter());
        assertEquals("#457b9d", loaded.sources().get(0).color());
        assertEquals("Privat", loaded.sources().get(1).name());
        assertEquals("", loaded.sources().get(1).filter());
        assertEquals("", loaded.sources().get(1).color());
        assertEquals(enabled, loaded.enabled());
        assertEquals("Urlaub", loaded.keyword());
        assertEquals(from, loaded.from());
        assertEquals(to, loaded.to());
        assertEquals(categories, loaded.categories());
    }

    @Test
    void roundTripsLayout(@TempDir Path dir) {
        Path file = dir.resolve("layout.properties");
        ConfigStore store = new ConfigStore(file);
        List<Double> widths = List.of(110.0, 90.0, 200.0, 120.0, 120.0);
        ConfigStore.Layout layout = new ConfigStore.Layout(0.8, 0.4, 100.0, 50.0, 1200.0, 800.0, widths);
        store.save(new ConfigStore.Data(List.of(), new LinkedHashSet<>(), "", null, null,
                new LinkedHashSet<>(), layout));

        ConfigStore.Layout loaded = store.load().layout();
        assertEquals(0.8, loaded.hDivider());
        assertEquals(0.4, loaded.vDivider());
        assertEquals(100.0, loaded.windowX());
        assertEquals(50.0, loaded.windowY());
        assertEquals(1200.0, loaded.windowWidth());
        assertEquals(800.0, loaded.windowHeight());
        assertEquals(widths, loaded.columnWidths());
    }

    @Test
    void loadReturnsDefaultsWhenFileMissing(@TempDir Path dir) {
        Path file = dir.resolve("does-not-exist.properties");
        ConfigStore store = new ConfigStore(file);

        ConfigStore.Data loaded = store.load();
        assertEquals(List.of(), loaded.sources());
        assertEquals(new LinkedHashSet<>(), loaded.enabled());
        assertEquals("", loaded.keyword());
        assertEquals(null, loaded.from());
        assertEquals(null, loaded.to());
        assertEquals(new LinkedHashSet<>(), loaded.categories());
    }
}
