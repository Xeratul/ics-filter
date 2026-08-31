package com.icsfilter.ui;

import com.icsfilter.model.CalendarEvent;
import com.icsfilter.model.CalendarSource;
import com.icsfilter.model.StartFrom;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A sorted table of the filtered events.
 */
public final class EventListPane extends VBox {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Label countLabel = new Label();
    private final TableView<CalendarEvent> table = new TableView<>();
    private final ObservableList<CalendarEvent> items = FXCollections.observableArrayList();
    private List<CalendarSource> sourceOrder = List.of();
    private List<CalendarEvent> lastEvents = List.of();
    private StartFrom startFrom = StartFrom.YEAR;
    private Consumer<StartFrom> onStartFromChanged = m -> { };
    private Consumer<CalendarEvent> onIgnoreRequest = e -> { };

    public EventListPane() {
        setSpacing(6);
        setPadding(new Insets(8));
        countLabel.setStyle("-fx-font-weight: bold;");

        TableColumn<CalendarEvent, CalendarEvent> dateCol = new TableColumn<>("Datum");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        dateCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(formatDate(event));
            }
        });
        dateCol.setPrefWidth(110);
        dateCol.setComparator(Comparator.comparing(CalendarEvent::startDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        TableColumn<CalendarEvent, CalendarEvent> timeCol = new TableColumn<>("Uhrzeit");
        timeCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        timeCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(event.allDay() ? "ganztägig" : formatTime(event));
            }
        });
        timeCol.setPrefWidth(90);
        timeCol.setComparator(Comparator.comparing(EventListPane::sortTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        TableColumn<CalendarEvent, CalendarEvent> titleCol = new TableColumn<>("Titel");
        titleCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        titleCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(stripParentheses(event.summary()));
                setTooltip(new javafx.scene.control.Tooltip(event.summary()));
            }
        });
        titleCol.setPrefWidth(200);
        titleCol.setComparator(Comparator.comparing(CalendarEvent::summary,
                Comparator.nullsLast(Comparator.naturalOrder())));

        TableColumn<CalendarEvent, CalendarEvent> sourceCol = new TableColumn<>("Quelle");
        sourceCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        sourceCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(event.source().name());
                CalendarSource current = UiPalette.currentSource(event.source(), sourceOrder);
                int idx = current == null ? -1 : sourceOrder.indexOf(current);
                Color color = current == null ? Color.web("#999") : UiPalette.resolveColor(current, idx < 0 ? 0 : idx);
                setStyle("-fx-text-fill: " + toCss(color) + ";");
            }
        });
        sourceCol.setPrefWidth(120);
        sourceCol.setComparator(Comparator.comparing(e -> e.source() == null ? "" : e.source().name(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        TableColumn<CalendarEvent, CalendarEvent> locationCol = new TableColumn<>("Ort");
        locationCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        locationCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(event.location());
            }
        });
        locationCol.setPrefWidth(120);
        locationCol.setComparator(Comparator.comparing(CalendarEvent::location,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        table.getColumns().addAll(dateCol, timeCol, titleCol, sourceCol, locationCol);
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> contextMenuRow());
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Button settingsButton = settingsButton();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(countLabel, spacer, settingsButton);

        getChildren().addAll(header, table);
    }

    /** The gear button in the top-right corner opens the settings dialog. */
    private Button settingsButton() {
        Button button = new Button("\u2699");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16; -fx-padding: 0 4 0 4;");
        button.setTooltip(new Tooltip("Einstellungen"));
        button.setOnAction(e -> openSettingsDialog());
        return button;
    }

    /** The current "start from" cutoff applied to the list. */
    public StartFrom startFrom() {
        return startFrom;
    }

    /** Sets the cutoff and re-applies it; notifies {@code onStartFromChanged}. */
    public void setStartFrom(StartFrom startFrom) {
        if (startFrom == null || this.startFrom == startFrom) {
            return;
        }
        this.startFrom = startFrom;
        refresh();
        onStartFromChanged.accept(startFrom);
    }

    public void setOnStartFromChanged(Consumer<StartFrom> onStartFromChanged) {
        this.onStartFromChanged = onStartFromChanged == null ? m -> { } : onStartFromChanged;
    }

    public void setOnEventSelected(Consumer<CalendarEvent> onEventSelected) {
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n != null) {
                        onEventSelected.accept(n);
                    }
                });
    }

    /** Registers a callback fired when the user requests ignoring an event's title. */
    public void setOnIgnoreRequest(Consumer<CalendarEvent> onIgnoreRequest) {
        this.onIgnoreRequest = onIgnoreRequest == null ? e -> { } : onIgnoreRequest;
    }

    /** A table row exposing a right-click menu to ignore the event's title. */
    private TableRow<CalendarEvent> contextMenuRow() {
        TableRow<CalendarEvent> row = new TableRow<>();
        ContextMenu menu = new ContextMenu();
        MenuItem ignore = new MenuItem("Diese Einträge ignorieren");
        ignore.setOnAction(e -> {
            CalendarEvent event = row.getItem();
            if (event != null) {
                onIgnoreRequest.accept(event);
            }
        });
        menu.getItems().add(ignore);
        row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
        return row;
    }

    /** Snapshot of the current column widths, in column order. */
    public List<Double> columnWidths() {
        List<Double> widths = new java.util.ArrayList<>();
        for (TableColumn<CalendarEvent, ?> col : table.getColumns()) {
            widths.add(col.getWidth());
        }
        return widths;
    }

    /** Restores saved column widths; ignored when the size does not match. */
    public void setColumnWidths(List<Double> widths) {
        if (widths == null || widths.size() != table.getColumns().size()) {
            return;
        }
        for (int i = 0; i < table.getColumns().size(); i++) {
            Double w = widths.get(i);
            if (w != null && w > 0) {
                table.getColumns().get(i).setPrefWidth(w);
            }
        }
    }

    /** Sets the canonical source order used to resolve stable source colours. */
    public void setSourceOrder(List<CalendarSource> sourceOrder) {
        this.sourceOrder = sourceOrder == null ? List.of() : sourceOrder;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.lastEvents = events == null ? List.of() : events;
        refresh();
    }

    /** Recomputes the matching rows (after the {@code startFrom} cutoff) and refreshes the label. */
    private void refresh() {
        LocalDate cutoff = startFrom.effectiveDate(LocalDate.now());
        List<CalendarEvent> sorted = new java.util.ArrayList<>(lastEvents.stream()
                .filter(e -> e.startDate() == null || !e.startDate().isBefore(cutoff))
                .toList());
        sorted.sort(Comparator.comparing(CalendarEvent::startDate)
                .thenComparing(e -> e.start() == null ? java.time.LocalDateTime.MIN
                        : e.start().toLocalDateTime()));
        items.setAll(sorted);
        countLabel.setText(sorted.size() + " Termine");
    }

    /** Opens the settings dialog to choose the "start from" cutoff. */
    private void openSettingsDialog() {
        ToggleGroup group = new ToggleGroup();
        RadioButton year = radio("Ab diesem Jahr", StartFrom.YEAR, group);
        RadioButton month = radio("Ab diesem Monat", StartFrom.MONTH, group);
        RadioButton today = radio("Ab heute", StartFrom.TODAY, group);
        select(group, startFrom);

        VBox box = new VBox(8,
                new Label("Zeige Termine ab:"),
                year, month, today);
        box.setPadding(new Insets(12));

        Dialog<StartFrom> dialog = new Dialog<>();
        dialog.setTitle("Einstellungen");
        ButtonType save = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);
        dialog.getDialogPane().setContent(box);
        dialog.setResultConverter(bt -> bt == save ? selected(group) : null);
        dialog.showAndWait().ifPresent(this::setStartFrom);
    }

    private static RadioButton radio(String label, StartFrom value, ToggleGroup group) {
        RadioButton rb = new RadioButton(label);
        rb.setToggleGroup(group);
        rb.setUserData(value);
        return rb;
    }

    private static void select(ToggleGroup group, StartFrom value) {
        for (Toggle toggle : group.getToggles()) {
            if (toggle.getUserData() == value) {
                toggle.setSelected(true);
                return;
            }
        }
    }

    private static StartFrom selected(ToggleGroup group) {
        Toggle selected = group.getSelectedToggle();
        if (selected != null && selected.getUserData() instanceof StartFrom s) {
            return s;
        }
        return StartFrom.YEAR;
    }

    /** Sorting key for the time column: all-day events sort first within a day. */
    private static LocalTime sortTime(CalendarEvent event) {
        if (event.allDay()) {
            return LocalTime.MIN;
        }
        return event.start() == null ? LocalTime.MAX : event.start().toLocalTime();
    }

    private String formatDate(CalendarEvent event) {
        if (event.startDate() == null) {
            return "";
        }
        return event.startDate().format(DATE_FMT);
    }

    private String formatTime(CalendarEvent event) {
        if (event.start() == null) {
            return "";
        }
        return event.start().toLocalTime().format(TIME_FMT);
    }

    private String toCss(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    /** Removes parenthesised fragments, e.g. "Meeting (Sitzung)" -> "Meeting". */
    private static String stripParentheses(String s) {
        if (s == null || s.isBlank()) {
            return s == null ? "" : s;
        }
        return s.replaceAll("\\([^)]*\\)", "").replaceAll("\\s{2,}", " ").trim();
    }
}
