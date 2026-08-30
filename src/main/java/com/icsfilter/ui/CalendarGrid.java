package com.icsfilter.ui;

import com.icsfilter.model.CalendarEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A month calendar grid that shows the events of each day as colored chips.
 */
public final class CalendarGrid extends BorderPane {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);
    private static final int CELL_H = 84;

    private final Label monthLabel = new Label();
    private final GridPane grid = new GridPane();
    private final List<Cell> cells = new ArrayList<>();

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private List<CalendarEvent> events = List.of();
    private Consumer<LocalDate> onDaySelected = d -> { };

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
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(prev, next, spacer, monthLabel, spacer, today);

        grid.setHgap(3);
        grid.setVgap(3);
        setTop(toolbar);
        setCenter(grid);
        rebuild();
    }

    public void setOnDaySelected(Consumer<LocalDate> onDaySelected) {
        this.onDaySelected = onDaySelected;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events == null ? List.of() : events;
        rebuild();
    }

    public void setSelectedDate(LocalDate date) {
        if (date == null) {
            return;
        }
        this.selected = date;
        YearMonth m = YearMonth.from(date);
        if (!m.equals(month)) {
            month = m;
        }
        rebuild();
    }

    private void navigate(int delta) {
        month = month.plusMonths(delta);
        rebuild();
    }

    private void goToToday() {
        selected = LocalDate.now();
        month = YearMonth.now();
        rebuild();
    }

    /** Builds a colour-index map so source colours stay stable across rebuilds. */
    private Map<String, Color> sourceColors() {
        Map<String, Color> colors = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (!names.contains(event.source().name())) {
                names.add(event.source().name());
            }
        }
        for (int i = 0; i < names.size(); i++) {
            colors.put(names.get(i), UiPalette.colorFor(i));
        }
        return colors;
    }

    /** Groups events by the calendar day they start on. */
    private Map<LocalDate, List<CalendarEvent>> byDay() {
        Map<LocalDate, List<CalendarEvent>> map = new LinkedHashMap<>();
        for (CalendarEvent event : events) {
            if (event.startDate() != null) {
                map.computeIfAbsent(event.startDate(), k -> new ArrayList<>()).add(event);
            }
        }
        return map;
    }

    private void rebuild() {
        grid.getChildren().clear();
        cells.clear();

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
                selected = date;
                month = YearMonth.from(date);
                onDaySelected.accept(date);
                rebuild();
            });
            cells.add(cell);
            grid.add(cell.node(), i % 7, 1 + i / 7);
        }
    }

    /** A single day cell. */
    private final class Cell {
        private final LocalDate date;
        private final boolean inMonth;
        private final StackPane root = new StackPane();
        private final VBox content = new VBox(2);
        private final Label dayLabel = new Label();

        Cell(LocalDate date, boolean inMonth, List<CalendarEvent> dayEvents, Map<String, Color> colors) {
            this.date = date;
            this.inMonth = inMonth;
            content.setPadding(new Insets(3));
            content.setAlignment(Pos.TOP_LEFT);
            dayLabel.setStyle("-fx-font-weight: bold;");
            content.getChildren().add(dayLabelBag(date, dayEvents.size()));

            int shown = 0;
            for (CalendarEvent e : dayEvents) {
                if (shown >= 3) {
                    Label more = new Label("+" + (dayEvents.size() - shown) + " mehr");
                    more.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");
                    content.getChildren().add(more);
                    break;
                }
                Label chip = new Label(e.summary());
                Color c = colors.getOrDefault(e.source().name(), Color.web("#999"));
                chip.setStyle("-fx-background-color: " + toCss(c) + "; -fx-text-fill: white; "
                        + "-fx-font-size: 9; -fx-padding: 1 4 1 4; -fx-background-radius: 4;");
                chip.setMaxWidth(Double.MAX_VALUE);
                content.getChildren().add(chip);
                shown++;
            }
            root.getChildren().add(content);
            root.setPrefSize(Region.USE_COMPUTED_SIZE, CELL_H);
            root.setMinSize(110, CELL_H);
            root.setMaxSize(Double.MAX_VALUE, CELL_H);
            updateStyle();
        }

        private Label dayLabelBag(LocalDate d, int count) {
            String text = String.valueOf(d.getDayOfMonth());
            if (count > 0) {
                text += "  \u2022";
            }
            dayLabel.setText(text);
            return dayLabel;
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
            root.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                    + "; -fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");
        }

        private String toCss(Color color) {
            return String.format("#%02x%02x%02x",
                    (int) Math.round(color.getRed() * 255),
                    (int) Math.round(color.getGreen() * 255),
                    (int) Math.round(color.getBlue() * 255));
        }

        StackPane node() {
            return root;
        }
    }
}
