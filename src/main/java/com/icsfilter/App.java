package com.icsfilter;

import com.icsfilter.filter.EventFilter;
import com.icsfilter.ical.EventLoader;
import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import com.icsfilter.store.ConfigStore;
import com.icsfilter.ui.CalendarGrid;
import com.icsfilter.ui.EventDetailPane;
import com.icsfilter.ui.EventListPane;
import com.icsfilter.ui.FilterPane;
import com.icsfilter.ui.SourceManagerPane;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JavaFX application: manages ICS sources, loads and filters their events, and
 * shows them in a month calendar plus a sorted event list.
 */
public final class App extends Application {

    private final EventLoader loader = new EventLoader();
    private final SourceManagerPane sourceManager = new SourceManagerPane();
    private final FilterPane filterPane = new FilterPane();
    private final CalendarGrid calendarGrid = new CalendarGrid();
    private final EventListPane eventList = new EventListPane();
    private final EventDetailPane detailPane = new EventDetailPane();
    private final ConfigStore store = new ConfigStore();

    private final Label status = new Label("Bereit");
    private final Label titleLabel = new Label("ICS Filter");

    private List<CalendarEvent> allEvents = List.of();
    private SplitPane center;
    private SplitPane calendar;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        BorderPane root = new BorderPane();

        VBox left = new VBox(6, sourceManager, new Separator(), filterPane);
        ScrollPane leftScroll = new ScrollPane(left);
        leftScroll.setFitToWidth(true);
        leftScroll.setPrefWidth(340);

        root.setLeft(leftScroll);

        calendar = new SplitPane(calendarGrid, detailPane);
        calendar.setOrientation(Orientation.VERTICAL);
        calendar.setDividerPositions(0.62);
        // Let the calendar area squeeze so the event list can be widened.
        calendar.setMinWidth(300);

        // Calendar area (with detail pane) on the left, event list on the right;
        // the divider between the two is draggable.
        center = new SplitPane(calendar, eventList);
        center.setOrientation(Orientation.HORIZONTAL);
        center.setDividerPositions(0.7);
        root.setCenter(center);

        BorderPane header = new BorderPane();
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        header.setLeft(titleLabel);
        status.setStyle("-fx-text-fill: #666;");
        header.setRight(status);
        root.setTop(header);

        sourceManager.setOnReload(this::reload);
        sourceManager.setOnSourcesChanged(this::persistAndRefresh);
        filterPane.setOnChange(this::persistAndRefresh);
        calendarGrid.setOnDaySelected(d -> { });
        calendarGrid.setOnEventSelected(detailPane::setEvent);
        eventList.setOnEventSelected(e -> {
            calendarGrid.setSelectedDate(e.startDate());
            detailPane.setEvent(e);
        });

        Scene scene = new Scene(root, 1180, 740);
        stage.setTitle("ICS Filter");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(e -> persistAndRefresh());
        restore();
    }

    /** Restores sources, enabled flags, filters and the window layout. */
    private void restore() {
        ConfigStore.Data data = store.load();
        sourceManager.sources().setAll(data.sources());
        sourceManager.enabled().clear();
        sourceManager.enabled().addAll(data.enabled());
        filterPane.keyword(data.keyword());
        filterPane.from(data.from());
        filterPane.to(data.to());
        filterPane.selectCategories(data.categories());
        sourceManager.refreshList();
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
                new ArrayList<>(sourceManager.sources()),
                new LinkedHashSet<>(sourceManager.enabled()),
                filterPane.keyword(),
                filterPane.from(),
                filterPane.to(),
                new LinkedHashSet<>(filterPane.selectedCategories()),
                currentLayout());
        store.save(data);
        refreshViews();
    }

    /** Downloads and parses all enabled sources in a background task. */
    private void reload() {
        List<CalendarSource> sources = new ArrayList<>(sourceManager.sources());
        Set<String> enabled = new LinkedHashSet<>(sourceManager.enabled());
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

    /** Recomputes the filtered events and updates both views. */
    private void refreshViews() {
        EventFilter filter = buildFilter();
        Set<String> enabled = sourceManager.enabled();

        List<CalendarEvent> available = allEvents.stream()
                .filter(e -> enabled.contains(e.source().name()))
                .filter(App::matchesSourceFilter)
                .toList();

        List<CalendarEvent> filtered = filter.apply(available);
        calendarGrid.setEvents(filtered);
        eventList.setEvents(filtered);

        Set<String> categories = allEvents.stream()
                .map(CalendarEvent::category)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        filterPane.setCategories(categories);
    }

    private EventFilter buildFilter() {
        EventFilter filter = new EventFilter();
        filter.keyword(filterPane.keyword());
        filter.from(filterPane.from());
        filter.to(filterPane.to());
        filter.categories().addAll(filterPane.selectedCategories());
        return filter;
    }

    /** Keeps only events whose summary contains their source's filter string. */
    private static boolean matchesSourceFilter(CalendarEvent e) {
        String filter = e.source().filterLower();
        if (filter.isEmpty()) {
            return true;
        }
        return e.summary().toLowerCase().contains(filter);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
