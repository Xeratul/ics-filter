package com.icsfilter.ui;

import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A calendar grid that shows the events of each day as colored chips, switchable between a
 * month view (six weeks) and a week view where each day is laid out like the day popup.
 */
public final class CalendarGrid extends BorderPane {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN);
    private static final int CELL_H = 84;
    /** Estimated height of one event chip including the 2px VBox gap. */
    private static final double CHIP_H = 15;
    /** Estimated height of the day-number header row (label + expand icon). */
    private static final double HEADER_H = 20;
    /** Size factor applied to the enlarged day popup relative to its tile. */
    private static final double EXPAND_SCALE = 3.0;
    /** Minimum pixels allocated per hour in the enlarged day view, so blocks stay readable. */
    private static final double MIN_DAY_HOUR_H = 22;
    /** Maximum pixels per hour in the enlarged day view. */
    private static final double MAX_DAY_HOUR_H = 44;
    /** Accent colour used for the selected day in week mode. */
    private static final String WEEK_ACCENT = "#2a9d8f";
    /** Target height (px) of the week body, used to size the hour scale before layout. */
    private static final double WEEK_TARGET_H = 420;
    /** Height allocated to the all-day chip row in week mode. */
    private static final double WEEK_ALLDAY_H = 20;
    /** Height of the week day-header row in week mode. */
    private static final double WEEK_HEADER_H = 24;
    /** Width of the shared time-gutter column in week mode. */
    private static final double WEEK_GUTTER_W = 40;
    /** Minimum width of a single day's timed track in week mode. */
    private static final double WEEK_MIN_TRACK_W = 100;

    private final Label monthLabel = new Label();
    private final GridPane grid = new GridPane();
    private final StackPane gridLayer = new StackPane();
    private final Pane expandOverlay = new Pane();
    private final List<Cell> cells = new ArrayList<>();
    private Node expandedPanel;

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    /** The date whose tile is currently enlarged, or {@code null}. */
    private LocalDate expandedDate;
    /** True when the grid shows a single week (each day like the day popup) instead of a month. */
    private boolean weekMode = false;
    /** Monday of the week currently shown in week mode. */
    private LocalDate weekStart = weekStart(LocalDate.now());
    /** Day-header labels of the current week view, restyled when the selection changes. */
    private final List<Label> weekDayLabels = new ArrayList<>();
    private final ToggleButton monthToggle = new ToggleButton("Monat");
    private final ToggleButton weekToggle = new ToggleButton("Woche");
    private List<CalendarEvent> events = List.of();
    private List<CalendarSource> sourceOrder = List.of();
    private Consumer<LocalDate> onDaySelected = d -> { };
    private Consumer<CalendarEvent> onEventSelected = e -> { };

    public CalendarGrid() {
        setPadding(new Insets(8));

        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button prev = new Button("‹");
        Button next = new Button("›");
        Button today = new Button("Heute");
        prev.setOnAction(e -> navigate(-1));
        next.setOnAction(e -> navigate(1));
        today.setOnAction(e -> goToToday());
        monthLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        ToggleGroup viewGroup = new ToggleGroup();
        monthToggle.setToggleGroup(viewGroup);
        weekToggle.setToggleGroup(viewGroup);
        monthToggle.setSelected(true);
        monthToggle.setOnAction(e -> setViewMode(false));
        weekToggle.setOnAction(e -> setViewMode(true));
        updateToggleStyles();

        toolbar.getChildren().addAll(prev, next, spacerLeft, monthLabel, spacerRight, today, monthToggle, weekToggle);

        grid.setHgap(3);
        grid.setVgap(3);
        // Force seven equal-width columns (one per weekday).
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setFillWidth(true);
            grid.getColumnConstraints().add(col);
        }
        // Header row is fixed; the six week rows grow to fill the available height,
        // so each day cell shows as many event chips as fit.
        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(20);
        headerRow.setPrefHeight(24);
        headerRow.setMaxHeight(24);
        headerRow.setVgrow(Priority.NEVER);
        grid.getRowConstraints().add(headerRow);
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            row.setFillHeight(true);
            grid.getRowConstraints().add(row);
        }
        // The grid sits under a transparent overlay layer that shows the enlarged day popup.
        gridLayer.setAlignment(Pos.TOP_LEFT);
        StackPane.setAlignment(grid, Pos.TOP_LEFT);
        StackPane.setAlignment(expandOverlay, Pos.TOP_LEFT);
        expandOverlay.setPickOnBounds(false); // let clicks outside the popup reach the grid again
        expandOverlay.prefWidthProperty().bind(grid.widthProperty());
        expandOverlay.prefHeightProperty().bind(grid.heightProperty());
        gridLayer.getChildren().addAll(grid, expandOverlay);

        setTop(toolbar);
        setCenter(gridLayer);
        // Once the grid has a real height, size the week hour scale to fill it.
        gridLayer.heightProperty().addListener((obs, oldH, newH) -> {
            if (weekMode && newH != null && newH.doubleValue() > 0) {
                rebuild();
            }
        });
        rebuild();
    }

    public void setOnDaySelected(Consumer<LocalDate> onDaySelected) {
        this.onDaySelected = onDaySelected;
    }

    public void setOnEventSelected(Consumer<CalendarEvent> onEventSelected) {
        this.onEventSelected = onEventSelected;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events == null ? List.of() : events;
        rebuild();
    }

    /** Sets the canonical source order used to resolve stable palette colours. */
    public void setSourceOrder(List<CalendarSource> sourceOrder) {
        this.sourceOrder = sourceOrder == null ? List.of() : sourceOrder;
    }

    public void setSelectedDate(LocalDate date) {
        if (date == null) {
            return;
        }
        this.selected = date;
        if (weekMode) {
            LocalDate newStart = weekStart(date);
            if (!newStart.equals(weekStart)) {
                weekStart = newStart;
                rebuild();
            } else {
                updateSelection();
            }
        } else {
            YearMonth m = YearMonth.from(date);
            boolean monthChanged = !m.equals(month);
            this.month = m;
            if (monthChanged) {
                rebuild();
            } else {
                updateSelection();
            }
        }
    }

    /** True when the grid shows the week instead of the month view. */
    public boolean isWeekMode() {
        return weekMode;
    }

    /** Switches between the month and week views, keeping the selected day in focus. */
    private void setViewMode(boolean week) {
        if (this.weekMode == week) {
            return;
        }
        this.weekMode = week;
        if (week) {
            weekStart = weekStart(selected);
        } else {
            month = YearMonth.from(selected);
        }
        updateToggleStyles();
        rebuild();
    }

    /** Highlights the active view-mode toggle and dims the inactive one. */
    private void updateToggleStyles() {
        String active = "-fx-background-color: #cdeeea; -fx-text-fill: #1f1f1f; -fx-font-size: 11; -fx-font-weight: bold;";
        String inactive = "-fx-font-size: 11;";
        monthToggle.setStyle(weekMode ? inactive : active);
        weekToggle.setStyle(weekMode ? active : inactive);
    }

    private void navigate(int delta) {
        if (weekMode) {
            weekStart = weekStart.plusWeeks(delta);
            rebuild();
        } else {
            month = month.plusMonths(delta);
            rebuild();
        }
    }

    private void goToToday() {
        selected = LocalDate.now();
        month = YearMonth.now();
        weekStart = weekStart(selected);
        rebuild();
    }

    /** Enlarges the tile for {@code date}, or returns it to normal size. */
    private void toggleExpand(LocalDate date) {
        if (expandedDate != null && expandedDate.equals(date)) {
            collapse();
        } else {
            expand(date);
        }
    }

    /** Opens a popup over the chosen day that lists all its events at normal font size. */
    private void expand(LocalDate date) {
        collapse();
        Cell cell = cellFor(date);
        if (cell == null) {
            return;
        }
        expandedDate = date;

        double w = cell.content.getWidth();
        double h = cell.content.getHeight();
        if (w <= 0) {
            w = 160;
        }
        if (h <= 0) {
            h = CELL_H;
        }
        double gridW = grid.getWidth();
        double gridH = grid.getHeight();
        double pw = Math.min(w * EXPAND_SCALE, gridW > 0 ? gridW : w * EXPAND_SCALE);
        double ph = Math.min(h * EXPAND_SCALE, gridH > 0 ? gridH : h * EXPAND_SCALE);
        VBox panel = buildPopup(date, cell.dayEvents, cell.sourceColors, pw, ph);
        double px = clamp(cell.content.getLayoutX() + (w - pw) / 2, 0, Math.max(0, gridW - pw));
        double py = clamp(cell.content.getLayoutY() + (h - ph) / 2, 0, Math.max(0, gridH - ph));
        panel.setLayoutX(px);
        panel.setLayoutY(py);
        panel.setPrefSize(pw, ph);
        panel.setMinSize(pw, ph);
        panel.setMaxSize(pw, ph);
        expandOverlay.getChildren().add(panel);
        expandedPanel = panel;

        panel.setOpacity(0);
        panel.setScaleX(0.9);
        panel.setScaleY(0.9);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), panel);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        fade.setToValue(1.0);
        scale.play();
        fade.play();
    }

    /** Removes the popup, if any, with a quick fade-out. */
    private void collapse() {
        if (expandedPanel == null) {
            expandedDate = null;
            return;
        }
        Node panel = expandedPanel;
        expandedDate = null;
        expandedPanel = null;
        FadeTransition fade = new FadeTransition(Duration.millis(120), panel);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> expandOverlay.getChildren().remove(panel));
        fade.play();
    }

    private Cell cellFor(LocalDate date) {
        for (Cell c : cells) {
            if (c.date.equals(date)) {
                return c;
            }
        }
        return null;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    /** Returns the Monday of the week containing {@code date}. */
    private static LocalDate weekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    private static String toCss(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    /** Builds the popup panel for a single day as a day-style view (times on the left). */
    private VBox buildPopup(LocalDate date, List<CalendarEvent> dayEvents, Map<String, Color> colors,
                            double popupW, double popupH) {
        VBox panel = new VBox(4);
        panel.setPadding(new Insets(6));
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #2a9d8f; "
                + "-fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        panel.setEffect(new DropShadow(14, Color.rgb(0, 0, 0, 0.35)));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(date.format(DAY_FMT));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #222;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("\u2715");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; "
                + "-fx-font-size: 12; -fx-cursor: hand;");
        close.setOnAction(e -> collapse());
        header.getChildren().addAll(title, spacer, close);

        Node dayView = buildDayView(dayEvents, colors, popupW, popupH);

        ScrollPane scroll = new ScrollPane(dayView);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);
        return panel;
    }

    /**
     * Creates a day view: a left time column with hour labels and a track on the right where each
     * event is a colored block placed at its start time and sized by its duration.
     */
    private Node buildDayView(List<CalendarEvent> dayEvents, Map<String, Color> colors,
                              double popupW, double availableH) {
        if (dayEvents.isEmpty()) {
            Label none = new Label("Keine Termine an diesem Tag.");
            none.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
            return none;
        }

        List<CalendarEvent> allDay = new ArrayList<>();
        List<CalendarEvent> timed = new ArrayList<>();
        for (CalendarEvent e : dayEvents) {
            if (e.allDay() || e.start() == null) {
                allDay.add(e);
            } else {
                timed.add(e);
            }
        }
        timed.sort(Comparator.comparingLong(e -> e.start().toLocalTime().toSecondOfDay()));

        // Only event blocks (timed / all-day) in a single VBox; the time column is drawn beside it.
        HBox day = new HBox();
        day.setAlignment(Pos.TOP_LEFT);

        double timeColW = 40;
        double trackW = Math.max(120, popupW - timeColW - 4);
        double hourH = MIN_DAY_HOUR_H;

        if (timed.isEmpty()) {
            // No timed events: show the all-day chips as full-width blocks.
            VBox allDayBox = timedTrack(allDay, colors, trackW);
            day.getChildren().add(allDayBox);
            return day;
        }

        // Determine the visible hour range covering all timed events.
        int startHour = timed.stream()
                .mapToInt(e -> e.start().getHour())
                .min().orElse(0);
        int endHour = timed.stream()
                .mapToInt(e -> e.end() == null ? e.start().getHour() + 1 : Math.max(e.end().getHour() + (e.end().getMinute() > 0 ? 1 : 0), e.start().getHour() + 1))
                .max().orElse(startHour + 1);
        startHour = Math.max(0, startHour);
        endHour = Math.min(24, Math.max(endHour, startHour + 1));
        int rangeHours = endHour - startHour;

        // Fit the whole day view into the available popup height (minus header + padding) so no
        // scrollbar appears. The hour scale grows up to a readable cap.
        double contentH = availableH > 0 ? Math.max(1, availableH - HEADER_H - 12) : rangeHours * MAX_DAY_HOUR_H;
        hourH = Math.min(MAX_DAY_HOUR_H, contentH / rangeHours);
        if (hourH < MIN_DAY_HOUR_H) {
            hourH = Math.max(hourH, 8); // never shrink below a barely readable slot
        }

        double trackH = rangeHours * hourH;

        VBox timeCol = new VBox();
        timeCol.setPrefWidth(timeColW);
        timeCol.setMinWidth(timeColW);
        timeCol.setMaxWidth(timeColW);
        for (int h = startHour; h <= endHour; h++) {
            Label lb = new Label(String.format("%02d:00", h));
            lb.setPrefWidth(timeColW);
            lb.setMinWidth(timeColW);
            lb.setPrefHeight(hourH);
            lb.setMinHeight(hourH);
            lb.setStyle("-fx-font-size: 8; -fx-text-fill: #888; -fx-padding: 0 4 0 0; -fx-alignment: top-right;");
            lb.setMaxHeight(Double.MAX_VALUE);
            timeCol.getChildren().add(lb);
        }
        // The time column is anchored to the top; labels are as tall as one hour slot.
        timeCol.setPrefHeight(trackH);
        timeCol.setMinHeight(trackH);

        Pane track = new Pane();
        track.setPrefSize(trackW, trackH);
        track.setMinSize(trackW, trackH);
        track.setMaxSize(trackW, trackH);
        addTimedBlocks(track, timed, colors, trackW, trackH, startHour, endHour, hourH);

        // All-day events appear as a full-width block above the time grid.
        if (!allDay.isEmpty()) {
            VBox allDayBox = timedTrack(allDay, colors, trackW);
            VBox wrap = new VBox(2, allDayBox, new HBox(timeCol, track));
            return wrap;
        }

        day.getChildren().addAll(timeCol, track);
        return day;
    }

    /** Builds a thin block listing all-day events for a full width. */
    private VBox timedTrack(List<CalendarEvent> allDay, Map<String, Color> colors, double trackW) {
        VBox box = new VBox(2);
        box.setPrefWidth(trackW);
        for (CalendarEvent e : allDay) {
            Color c = colors.getOrDefault(e.source().name(), Color.web("#999"));
            Label chip = new Label(e.summary());
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setWrapText(true);
            chip.setStyle("-fx-background-color: " + toCss(c) + "; -fx-text-fill: white; "
                    + "-fx-font-size: 9; -fx-padding: 1 4 1 4; -fx-background-radius: 4;");
            chip.setOnMouseClicked(ev -> onEventSelected.accept(e));
            box.getChildren().add(chip);
        }
        return box;
    }

    /**
     * Paints the hour gridlines and the timed event blocks into {@code track}. Blocks are placed at
     * their start time and sized by duration; overlapping events are pushed into side-by-side columns
     * using the classic greedy column assignment.
     */
    private void addTimedBlocks(Pane track, List<CalendarEvent> timed, Map<String, Color> colors,
                                double trackW, double trackH, int startHour, int endHour, double hourH) {
        // Hour gridlines.
        for (int h = startHour; h < endHour; h++) {
            Region line = new Region();
            line.setLayoutY((h - startHour + 1) * hourH - 0.5);
            line.setPrefWidth(trackW);
            line.setPrefHeight(1);
            line.setStyle("-fx-background-color: #eee;");
            track.getChildren().add(line);
        }

        List<int[]> minutes = new ArrayList<>();
        for (CalendarEvent e : timed) {
            int sm = e.start().getHour() * 60 + e.start().getMinute();
            int em = e.end() == null ? sm + 60 : e.end().getHour() * 60 + e.end().getMinute();
            minutes.add(new int[]{sm, em});
        }
        List<Integer> colOf = new ArrayList<>();
        List<Integer> colEnd = new ArrayList<>();
        int maxCols = 1;
        for (int i = 0; i < timed.size(); i++) {
            int sm = minutes.get(i)[0];
            int em = minutes.get(i)[1];
            int col = -1;
            for (int c = 0; c < colEnd.size(); c++) {
                // Reuse a column if the last event placed there ends before this one starts.
                if (colEnd.get(c) <= sm) {
                    col = c;
                    break;
                }
            }
            if (col < 0) {
                col = colEnd.size();
                colEnd.add(em);
            } else {
                colEnd.set(col, Math.max(colEnd.get(col), em));
            }
            colOf.add(col);
            maxCols = Math.max(maxCols, col + 1);
        }

        double colGap = 2;
        double colW = (trackW - 3 - (maxCols - 1) * colGap) / maxCols;
        for (int i = 0; i < timed.size(); i++) {
            CalendarEvent e = timed.get(i);
            double y = (minutes.get(i)[0] * 60 - startHour * 3600) / 3600.0 * hourH;
            double hblk = Math.min(trackH - y,
                    Math.max(12, (minutes.get(i)[1] - minutes.get(i)[0]) / 60.0 * hourH));
            if (hblk < 12) {
                hblk = Math.min(12, trackH - y);
            }
            Color c = colors.getOrDefault(e.source().name(), Color.web("#999"));
            Label blk = new Label(e.summary());
            blk.setLayoutX(colOf.get(i) * (colW + colGap));
            blk.setLayoutY(y);
            blk.setPrefSize(colW, hblk);
            blk.setMaxWidth(colW);
            blk.setWrapText(true);
            blk.setAlignment(Pos.TOP_LEFT); // keep the text at the top of the block
            blk.setStyle("-fx-background-color: " + toCss(c) + "; -fx-text-fill: white; "
                    + "-fx-font-size: 9; -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
            blk.setOnMouseClicked(ev -> onEventSelected.accept(e));
            track.getChildren().add(blk);
        }
    }

    /** Updates the selection highlight and expand icons in place, without rebuilding the grid. */
    private void updateSelection() {
        // Collapse a previously enlarged tile that is no longer the selected day.
        if (expandedDate != null && !expandedDate.equals(selected)) {
            collapse();
        }
        for (Cell cell : cells) {
            cell.refreshSelection();
        }
        if (weekMode) {
            refreshWeekHeaders();
        }
    }

    /** Builds a colour map so source colours stay stable across rebuilds. */
    private Map<String, Color> sourceColors() {
        Map<String, Color> colors = new LinkedHashMap<>();
        for (CalendarEvent event : events) {
            if (event.source() == null) {
                continue;
            }
            String name = event.source().name();
            if (!colors.containsKey(name)) {
                CalendarSource current = UiPalette.currentSource(event.source(), sourceOrder);
                int idx = current == null ? -1 : sourceOrder.indexOf(current);
                colors.put(name, UiPalette.resolveColor(current, idx < 0 ? colors.size() : idx));
            }
        }
        return colors;
    }

    /** Groups events by every calendar day they occupy, so multi-day events span their whole range. */
    private Map<LocalDate, List<CalendarEvent>> byDay() {
        Map<LocalDate, List<CalendarEvent>> map = new LinkedHashMap<>();
        for (CalendarEvent event : events) {
            LocalDate first = event.startDate();
            if (first == null) {
                continue;
            }
            LocalDate last = event.lastDay() == null ? first : event.lastDay();
            for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
                map.computeIfAbsent(day, k -> new ArrayList<>()).add(event);
            }
        }
        map.values().forEach(list -> list.sort(CalendarGrid::byStartTime));
        return map;
    }

    /** Orders events within a day by their start time; all-day events come first. */
    private static int byStartTime(CalendarEvent a, CalendarEvent b) {
        int byKind = Boolean.compare(b.allDay(), a.allDay());
        if (byKind != 0) {
            return byKind;
        }
        LocalTime at = a.allDay() ? LocalTime.MIN : a.start() == null ? LocalTime.MAX : a.start().toLocalTime();
        LocalTime bt = b.allDay() ? LocalTime.MIN : b.start() == null ? LocalTime.MAX : b.start().toLocalTime();
        return at.compareTo(bt);
    }

    /** Rebuilds the calendar according to the current view mode (month or week). */
    private void rebuild() {
        expandedDate = null;
        expandedPanel = null;
        expandOverlay.getChildren().clear();
        grid.getChildren().clear();
        cells.clear();
        weekDayLabels.clear();
        if (weekMode) {
            buildWeek();
        } else {
            buildMonth();
        }
    }

    /** Builds the six-week month grid with clickable day chips. */
    private void buildMonth() {
        monthLabel.setText(month.format(MONTH_FMT));
        Map<LocalDate, List<CalendarEvent>> dayMap = byDay();
        Map<String, Color> colors = sourceColors();

        String[] dayNames = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        for (int col = 0; col < 7; col++) {
            Label header = new Label(dayNames[col]);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-padding: 2;");
            grid.add(header, col, 0);
            GridPane.setHgrow(header, Priority.ALWAYS);
        }

        LocalDate first = month.atDay(1);
        int leading = first.getDayOfWeek().getValue() - 1; // Monday=0
        LocalDate gridStart = first.minusDays(leading);

        for (int i = 0; i < 42; i++) {
            LocalDate date = gridStart.plusDays(i);
            boolean inMonth = YearMonth.from(date).equals(month);
            Cell cell = new Cell(date, inMonth, dayMap.getOrDefault(date, List.of()), colors);
            cell.node().setOnMouseClicked(e -> {
                YearMonth clickedMonth = YearMonth.from(date);
                boolean monthChanged = !clickedMonth.equals(month);
                selected = date;
                month = clickedMonth;
                onDaySelected.accept(date);
                if (monthChanged) {
                    rebuild();
                } else {
                    updateSelection();
                }
            });
            cells.add(cell);
            grid.add(cell.node(), i % 7, 1 + i / 7);
        }

        gridLayer.getChildren().setAll(grid, expandOverlay);
    }

    /** Builds the single-week view where each day is laid out like the day popup. */
    private void buildWeek() {
        monthLabel.setText(weekLabel());
        Map<LocalDate, List<CalendarEvent>> dayMap = byDay();
        Map<String, Color> colors = sourceColors();
        ScrollPane week = buildWeekGrid(dayMap, colors);
        gridLayer.getChildren().setAll(week);
    }

    /** A compact German range label for the currently displayed week. */
    private String weekLabel() {
        LocalDate end = weekStart.plusDays(6);
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN);
        if (weekStart.getYear() == end.getYear()) {
            return weekStart.format(shortFmt) + " – " + end.format(shortFmt) + " " + end.getYear();
        }
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN);
        return weekStart.format(fullFmt) + " – " + end.format(fullFmt);
    }

    /** Renders the week grid: a shared time gutter plus seven day columns. */
    private ScrollPane buildWeekGrid(Map<LocalDate, List<CalendarEvent>> dayMap, Map<String, Color> colors) {
        LocalDate[] days = new LocalDate[7];
        for (int i = 0; i < 7; i++) {
            days[i] = weekStart.plusDays(i);
        }

        // Collect timed events across the week to compute a shared hour range.
        List<CalendarEvent> allTimed = new ArrayList<>();
        for (LocalDate d : days) {
            for (CalendarEvent e : dayMap.getOrDefault(d, List.of())) {
                if (!(e.allDay() || e.start() == null)) {
                    allTimed.add(e);
                }
            }
        }
        int[] range = hourRange(allTimed);
        int startHour = range[0];
        int endHour = range[1];
        int rangeHours = endHour - startHour;

        // Vertical scale so the week fills the available body height.
        double bodyH = gridLayer.getHeight() > 0 ? gridLayer.getHeight() : WEEK_TARGET_H;
        double available = bodyH - WEEK_HEADER_H - WEEK_ALLDAY_H - 8; // minus header row, all-day row, padding
        double hourH = Math.max(8, Math.min(MAX_DAY_HOUR_H, available / rangeHours));
        double trackH = rangeHours * hourH;

        GridPane weekGrid = new GridPane();
        weekGrid.setPadding(new Insets(4));
        weekGrid.setHgap(2);
        weekGrid.setVgap(2);
        weekGrid.setStyle("-fx-background-color: #ffffff;");

        // Top row: day-header labels (with the selected day shown). Cell (0,0) holds a spacer for the gutter.
        Label corner = new Label("");
        corner.setMinWidth(WEEK_GUTTER_W);
        corner.setMaxWidth(WEEK_GUTTER_W);
        corner.setPrefWidth(WEEK_GUTTER_W);
        weekGrid.add(corner, 0, 0);
        GridPane.setHgrow(corner, Priority.NEVER);

        String[] dayNames = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        for (int col = 0; col < 7; col++) {
            LocalDate d = days[col];
            Label header = buildWeekHeader(d, dayNames[col]);
            header.setMinWidth(WEEK_MIN_TRACK_W);
            header.setPrefWidth(WEEK_MIN_TRACK_W);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setOnMouseClicked(e -> {
                selected = d;
                onDaySelected.accept(d);
                updateSelection();
            });
            weekGrid.add(header, col + 1, 0);
            GridPane.setHgrow(header, Priority.ALWAYS);
            weekDayLabels.add(header);
        }

        // Middle row: each day's all-day chips, aligned above its timed track.
        for (int col = 0; col < 7; col++) {
            VBox allDay = buildAllDayChips(days[col], dayMap.getOrDefault(days[col], List.of()), colors);
            allDay.setMinWidth(WEEK_MIN_TRACK_W);
            allDay.setPrefWidth(WEEK_MIN_TRACK_W);
            allDay.setMaxWidth(Double.MAX_VALUE);
            weekGrid.add(allDay, col + 1, 1);
            GridPane.setHgrow(allDay, Priority.ALWAYS);
        }
        weekGrid.add(new Region(), 0, 1); // spacer under the gutter heading

        // Body row: shared time gutter + one track per day.
        Pane gutter = buildWeekGutter(startHour, endHour, hourH, trackH);
        weekGrid.add(gutter, 0, 2);
        GridPane.setHgrow(gutter, Priority.NEVER);

        for (int col = 0; col < 7; col++) {
            List<CalendarEvent> timed = dayMap.getOrDefault(days[col], List.of()).stream()
                    .filter(e -> !(e.allDay() || e.start() == null))
                    .collect(Collectors.toList());
            Pane track = new Pane();
            track.setMinWidth(WEEK_MIN_TRACK_W);
            track.setPrefWidth(WEEK_MIN_TRACK_W);
            track.setMaxWidth(Double.MAX_VALUE);
            track.setMinHeight(trackH);
            track.setPrefHeight(trackH);
            track.setMaxHeight(trackH);
            GridPane.setHgrow(track, Priority.ALWAYS);
            addTimedBlocks(track, timed, colors, track.getPrefWidth(), trackH, startHour, endHour, hourH);
            weekGrid.add(track, col + 1, 2);
            GridPane.setHgrow(track, Priority.ALWAYS);
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(WEEK_HEADER_H);
        headerRow.setPrefHeight(WEEK_HEADER_H);
        weekGrid.getRowConstraints().add(headerRow);

        RowConstraints allDayRow = new RowConstraints();
        allDayRow.setMinHeight(WEEK_ALLDAY_H);
        allDayRow.setPrefHeight(WEEK_ALLDAY_H);
        weekGrid.getRowConstraints().add(allDayRow);

        RowConstraints bodyRow = new RowConstraints();
        bodyRow.setVgrow(Priority.ALWAYS);
        bodyRow.setMinHeight(trackH);
        bodyRow.setPrefHeight(trackH);
        weekGrid.getRowConstraints().add(bodyRow);

        ScrollPane scroll = new ScrollPane(weekGrid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #ffffff; -fx-background-insets: 0;");
        scroll.setPannable(true);
        return scroll;
    }

    /** A bold day-header listing the weekday name and date number. */
    private Label buildWeekHeader(LocalDate date, String dayName) {
        Label header = new Label(dayName + "  " + date.getDayOfMonth());
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle(weekHeaderStyle(date));
        return header;
    }

    /** CSS for a week day-header, highlighting the selected day (and today). */
    private String weekHeaderStyle(LocalDate date) {
        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selected);
        String bg = isSelected ? WEEK_ACCENT : isToday ? "rgba(43,157,143,0.18)" : "transparent";
        return "-fx-font-weight: bold; -fx-text-fill: " + (isSelected ? "#fff" : "#4b5563")
                + "; -fx-background-color: " + bg + "; -fx-background-radius: 6; -fx-padding: 4;";
    }

    /** Re-applies the selected-day highlight to the week day-headers in place. */
    private void refreshWeekHeaders() {
        for (int i = 0; i < weekDayLabels.size() && i < 7; i++) {
            weekDayLabels.get(i).setStyle(weekHeaderStyle(weekStart.plusDays(i)));
        }
    }

    /** Builds the shared time gutter (hour labels) for the week body. */
    private Pane buildWeekGutter(int startHour, int endHour, double hourH, double trackH) {
        Pane pane = new Pane();
        pane.setMinWidth(WEEK_GUTTER_W);
        pane.setMaxWidth(WEEK_GUTTER_W);
        pane.setPrefWidth(WEEK_GUTTER_W);
        pane.setMinHeight(trackH);
        pane.setPrefHeight(trackH);
        pane.setMaxHeight(trackH);
        for (int h = startHour; h < endHour; h++) {
            Label label = new Label(String.format("%02d", h));
            label.getStyleClass().add("hour-label");
            label.setAlignment(Pos.CENTER_RIGHT);
            label.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280;");
            label.setPrefWidth(WEEK_GUTTER_W - 4);
            label.setLayoutY((h - startHour) * hourH - 6);
            pane.getChildren().add(label);
        }
        return pane;
    }

    /** The all-day chip stack for a single week day. */
    private VBox buildAllDayChips(LocalDate date, List<CalendarEvent> dayEvents, Map<String, Color> colors) {
        VBox box = new VBox(2);
        box.setMaxWidth(Double.MAX_VALUE);
        List<CalendarEvent> allDay = dayEvents.stream().filter(CalendarEvent::allDay).collect(Collectors.toList());
        int shown = Math.min(allDay.size(), 2);
        for (int i = 0; i < shown; i++) {
            CalendarEvent e = allDay.get(i);
            Label chip = new Label(e.summary() == null || e.summary().isBlank() ? "(ohne Titel)" : e.summary());
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setStyle("-fx-background-color: " + toCss(colors.getOrDefault(e.source().name(), Color.web("#999")))
                    + "; -fx-background-radius: 4;"
                    + " -fx-text-fill: white;"
                    + " -fx-font-size: 9; -fx-padding: 1 4;");
            box.getChildren().add(chip);
        }
        if (allDay.size() > shown) {
            Label more = new Label("+" + (allDay.size() - shown));
            more.setStyle("-fx-font-size: 9; -fx-text-fill: #6b7280;");
            box.getChildren().add(more);
        }
        return box;
    }

    /** Week mode always shows a fixed 8:00–20:00 range so the time gutter stays stable. */
    private static int[] hourRange(List<CalendarEvent> timed) {
        return new int[]{8, 20};
    }

    /** A single day cell. */
    private final class Cell {
        private final LocalDate date;
        private final boolean inMonth;
        private final VBox content = new VBox(2);
        private final HBox header = new HBox();
        private final Label dayLabel = new Label();
        private final Button expandIcon = new Button();
        private final List<CalendarEvent> dayEvents;
        private final Map<String, Color> sourceColors;

        Cell(LocalDate date, boolean inMonth, List<CalendarEvent> dayEvents, Map<String, Color> colors) {
            this.date = date;
            this.inMonth = inMonth;
            this.dayEvents = dayEvents;
            this.sourceColors = colors;

            content.setPadding(new Insets(3));
            content.setAlignment(Pos.TOP_LEFT);
            content.setMaxWidth(Double.MAX_VALUE);
            content.setMaxHeight(Double.MAX_VALUE);

            header.setAlignment(Pos.CENTER_LEFT);
            header.setMaxWidth(Double.MAX_VALUE);
            // The label must be allowed to grow to its max width, otherwise it sticks to its
            // preferred (small) size and the icon ends up right next to it instead of at the edge.
            dayLabel.setText(date.getDayOfMonth() + (dayEvents.isEmpty() ? "" : "  \u2022"));
            dayLabel.setStyle("-fx-font-weight: bold;");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(dayLabel, Priority.ALWAYS);

            // A crisp "maximize" icon (two opposite corner brackets) drawn as a path so it renders
            // identically regardless of the platform font.
            SVGPath icon = new SVGPath();
            icon.setContent("M6 2 H10 V6 H8 V4 H6 Z M6 10 H2 V6 H4 V8 H6 Z");
            icon.setFill(Color.web("#2a9d8f"));
            expandIcon.setGraphic(icon);
            expandIcon.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 3; "
                    + "-fx-padding: 1 3 1 3; -fx-cursor: hand;");
            expandIcon.setOnMouseClicked(e -> {
                e.consume();
                CalendarGrid.this.toggleExpand(date);
            });

            header.getChildren().addAll(dayLabel, expandIcon);
            content.getChildren().add(header);

            refreshSelection();
            renderChips();
            // Recompute chips whenever the row height changes so we use the vertical space.
            content.heightProperty().addListener((obs, ov, nv) -> renderChips());
        }

        /** Shows as many event chips as fit into the current cell height, plus a "+N mehr" marker. */
        private void renderChips() {
            // Drop anything added on a previous pass (keep the header at index 0).
            int childCount = content.getChildren().size();
            if (childCount > 1) {
                content.getChildren().remove(1, childCount);
            }

            double available = cellHeight() - content.getPadding().getTop() - content.getPadding().getBottom()
                    - HEADER_H - content.getSpacing();
            int fit = (int) Math.floor(available / CHIP_H);
            if (fit < 0) {
                fit = 0;
            }
            if (fit > dayEvents.size()) {
                fit = dayEvents.size();
            }
            int shown = fit;
            if (dayEvents.size() > shown) {
                // Reserve one slot for the "+N mehr" marker.
                if (shown > 0) {
                    shown--;
                }
            }

            for (int i = 0; i < shown; i++) {
                CalendarEvent e = dayEvents.get(i);
                Color c = sourceColors.getOrDefault(e.source().name(), Color.web("#999"));
                Label chip = new Label(e.summary());
                chip.setStyle("-fx-background-color: " + toCss(c) + "; -fx-text-fill: white; "
                        + "-fx-font-size: 9; -fx-padding: 1 4 1 4; -fx-background-radius: 4;");
                chip.setMaxWidth(Double.MAX_VALUE);
                chip.setOnMouseClicked(ev -> onEventSelected.accept(e));
                content.getChildren().add(chip);
            }
            if (dayEvents.size() > shown) {
                Label more = new Label("+" + (dayEvents.size() - shown) + " mehr");
                more.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");
                content.getChildren().add(more);
            }
        }

        private double cellHeight() {
            double h = content.getHeight();
            return h > 0 ? h : CELL_H;
        }

        /** Re-evaluates the selection highlight and expand-icon visibility for this cell. */
        private void refreshSelection() {
            boolean isSelected = date.equals(CalendarGrid.this.selected);
            if (expandIcon.isVisible() != isSelected) {
                expandIcon.setVisible(isSelected);
                expandIcon.setManaged(isSelected);
            }
            updateStyle();
        }

        private void updateStyle() {
            String bg = inMonth ? "#ffffff" : "#f2f2f2";
            String textColor = inMonth ? "#222" : "#aaa";
            String border = "#dddddd";
            if (!inMonth) {
                textColor = "#bbbbbb";
            }
            if (date.equals(LocalDate.now())) {
                bg = "#eaf6f5";
                border = "#2a9d8f";
            }
            if (date.equals(selected)) {
                bg = "#cdeeea";
                border = "#2a9d8f";
            }
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
            content.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                    + "; -fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");
        }

        VBox node() {
            return content;
        }
    }
}
