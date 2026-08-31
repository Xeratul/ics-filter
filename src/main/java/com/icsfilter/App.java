package com.icsfilter;

import com.icsfilter.ical.EventLoader;
import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import com.icsfilter.model.StartFrom;
import com.icsfilter.store.ConfigStore;
import com.icsfilter.ui.CalendarGrid;
import com.icsfilter.ui.EventDetailPane;
import com.icsfilter.ui.EventListPane;
import com.icsfilter.ui.SourceTilesBar;
import com.icsfilter.ui.UiPalette;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JavaFX application: manages ICS sources, loads their events, and shows them
 * in a month calendar plus a sorted event list.
 */
public final class App extends Application {

    private final EventLoader loader = new EventLoader();
    private final SourceTilesBar sourceTiles = new SourceTilesBar();
    private final CalendarGrid calendarGrid = new CalendarGrid();
    private final EventListPane eventList = new EventListPane();
    private final EventDetailPane detailPane = new EventDetailPane();
    private final ConfigStore store = new ConfigStore();

    private final Label status = new Label("Bereit");
    private final Label titleLabel = new Label("ICS Filter");

    private List<CalendarEvent> allEvents = List.of();

    // Persistent global-filter state (keyword/date range/categories). The UI does
    // not edit these yet, but we preserve any values from the config file so that
    // saving here never wipes them.
    private String keyword = "";
    private LocalDate from;
    private LocalDate to;
    private Set<String> categories = Set.of();
    private StartFrom startFrom = StartFrom.YEAR;

    private SplitPane center;
    private SplitPane calendar;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        BorderPane root = new BorderPane();

        // Calendar area (with detail pane) on the left, event list on the right.
        calendar = new SplitPane(calendarGrid, detailPane);
        calendar.setOrientation(Orientation.VERTICAL);
        calendar.setDividerPositions(0.62);
        // Let the calendar area squeeze so the event list can be widened.
        calendar.setMinWidth(300);

        center = new SplitPane(calendar, eventList);
        center.setOrientation(Orientation.HORIZONTAL);
        center.setDividerPositions(0.7);
        root.setCenter(center);

        // Top: header (title + status + reload) and the source tiles bar.
        BorderPane header = new BorderPane();
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        header.setLeft(titleLabel);
        status.setStyle("-fx-text-fill: #666;");
        Button reloadButton = new Button("Aktualisieren");
        reloadButton.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;");
        reloadButton.setOnAction(e -> reload());
        HBox headerRight = new HBox(8, status, reloadButton);
        headerRight.setAlignment(Pos.CENTER_RIGHT);
        header.setRight(headerRight);
        header.setPadding(new Insets(8));

        VBox top = new VBox(header, sourceTiles);
        root.setTop(top);

        sourceTiles.setOnSourcesChanged(this::persistAndRefresh);
        calendarGrid.setOnDaySelected(d -> { });
        calendarGrid.setOnEventSelected(detailPane::setEvent);
        eventList.setOnEventSelected(e -> {
            calendarGrid.setSelectedDate(e.startDate());
            detailPane.setEvent(e);
        });
        eventList.setOnIgnoreRequest(e -> sourceTiles.ignoreTitle(e.source(), e.summary()));

        Scene scene = new Scene(root, 1180, 740);
        stage.setTitle("ICS Filter");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(e -> persistAndRefresh());
        restore();
        // Wire the "start from" change callback after restore() so that applying
        // the persisted value during startup does not trigger a redundant save.
        eventList.setOnStartFromChanged(mode -> {
            this.startFrom = mode;
            persistAndRefresh();
        });
    }

    /** Restores sources, enabled flags, filters and the window layout. */
    private void restore() {
        ConfigStore.Data data = store.load();
        sourceTiles.sources().setAll(data.sources());
        sourceTiles.enabled().clear();
        sourceTiles.enabled().addAll(data.enabled());
        keyword = data.keyword();
        from = data.from();
        to = data.to();
        categories = data.categories();
        startFrom = parseStartFrom(data.startFrom());
        eventList.setStartFrom(startFrom);
        sourceTiles.refresh();
        restoreLayout(data.layout());
        refreshViews();
    }

    /** Applies the persisted split dividers, window geometry and column widths. */
    private void restoreLayout(ConfigStore.Layout layout) {
        center.setDividerPositions(layout.hDivider());
        calendar.setDividerPositions(layout.vDivider());
        if (!Double.isNaN(layout.windowX()) && !Double.isNaN(layout.windowY())) {
            stage.setX(layout.windowX());
            stage.setY(layout.windowY());
        }
        if (layout.windowWidth() > 0) {
            stage.setWidth(layout.windowWidth());
        }
        if (layout.windowHeight() > 0) {
            stage.setHeight(layout.windowHeight());
        }
        eventList.setColumnWidths(layout.columnWidths());
    }

    /** Captures the current split dividers, window geometry and column widths. */
    private ConfigStore.Layout currentLayout() {
        double h = center.getDividerPositions().length > 0 ? center.getDividerPositions()[0] : 0.7;
        double v = calendar.getDividerPositions().length > 0 ? calendar.getDividerPositions()[0] : 0.62;
        return new ConfigStore.Layout(h, v, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(),
                eventList.columnWidths());
    }

    /** Persists the current state, then updates both views. */
    private void persistAndRefresh() {
        ConfigStore.Data data = new ConfigStore.Data(
                new ArrayList<>(sourceTiles.sources()),
                new LinkedHashSet<>(sourceTiles.enabled()),
                keyword, from, to, categories,
                startFrom.name(), currentLayout());
        store.save(data);
        refreshViews();
    }

    /** Downloads and parses all enabled sources in a background task. */
    private void reload() {
        List<CalendarSource> sources = new ArrayList<>(sourceTiles.sources());
        Set<String> enabled = new LinkedHashSet<>(sourceTiles.enabled());
        List<CalendarSource> toLoad = sources.stream()
                .filter(s -> enabled.contains(s.name()))
                .toList();

        if (toLoad.isEmpty()) {
            allEvents = List.of();
            status.setText("Keine Quellen");
            refreshViews();
            return;
        }

        status.setText("Lade " + toLoad.size() + " Quelle(n) ...");
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime winStart = now.minusYears(1);
        ZonedDateTime winEnd = now.plusYears(2);

        Task<List<CalendarEvent>> task = new Task<>() {
            @Override
            protected List<CalendarEvent> call() {
                return loader.loadAll(toLoad, winStart, winEnd);
            }
        };
        task.setOnSucceeded(e -> {
            allEvents = task.getValue();
            status.setText("Geladen: " + allEvents.size() + " Termine");
            refreshViews();
        });
        task.setOnFailed(e -> {
            status.setText("Fehler beim Laden");
            task.getException().printStackTrace();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Recomputes the visible events and updates both views. */
    private void refreshViews() {
        Set<String> enabled = sourceTiles.enabled();
        // Canonical source order, used everywhere to resolve stable source colours.
        List<CalendarSource> sources = new ArrayList<>(sourceTiles.sources());

        List<CalendarEvent> visible = allEvents.stream()
                .filter(e -> enabled.contains(e.source().name()))
                .filter(e -> !isIgnored(e, sources))
                .filter(App::matchesSourceFilter)
                .toList();

        calendarGrid.setSourceOrder(sources);
        eventList.setSourceOrder(sources);
        detailPane.setSourceOrder(sources);
        calendarGrid.setEvents(visible);
        eventList.setEvents(visible);
    }

    /** Parses the persisted {@code startFrom} mode, falling back to the default. */
    private static StartFrom parseStartFrom(String s) {
        if (s == null) {
            return StartFrom.YEAR;
        }
        try {
            return StartFrom.valueOf(s.trim());
        } catch (RuntimeException e) {
            return StartFrom.YEAR;
        }
    }

    /** Keeps only events whose summary contains their source's filter string. */
    private static boolean matchesSourceFilter(CalendarEvent e) {
        String filter = e.source().filterLower();
        if (filter.isEmpty()) {
            return true;
        }
        return e.summary().toLowerCase().contains(filter);
    }

    /** True when the event's source (resolved by name) lists its exact summary as ignored. */
    private static boolean isIgnored(CalendarEvent e, List<CalendarSource> sources) {
        CalendarSource current = UiPalette.currentSource(e.source(), sources);
        return current != null && current.ignores(e.summary());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
