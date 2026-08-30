package com.icsfilter.ui;

import com.icsfilter.model.CalendarEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
                setText(event.summary());
                setTooltip(new javafx.scene.control.Tooltip(event.summary()));
            }
        });
        titleCol.setPrefWidth(200);

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
                int idx = items.indexOf(event);
                Color color = UiPalette.colorFor(idx < 0 ? 0 : idx);
                setStyle("-fx-text-fill: " + toCss(color) + ";");
            }
        });
        sourceCol.setPrefWidth(120);

        TableColumn<CalendarEvent, CalendarEvent> catCol = new TableColumn<>("Kategorie");
        catCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        catCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(CalendarEvent event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                setText(event.category() == null ? "" : event.category());
            }
        });
        catCol.setPrefWidth(120);

        table.getColumns().addAll(dateCol, timeCol, titleCol, sourceCol, catCol);
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(countLabel, table);
    }

    public void setOnEventSelected(Consumer<CalendarEvent> onEventSelected) {
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n != null) {
                        onEventSelected.accept(n);
                    }
                });
    }

    public void setEvents(List<CalendarEvent> events) {
        List<CalendarEvent> sorted = new java.util.ArrayList<>(events == null ? List.of() : events);
        sorted.sort(Comparator.comparing(CalendarEvent::startDate)
                .thenComparing(e -> e.start() == null ? java.time.LocalDateTime.MIN
                        : e.start().toLocalDateTime()));
        items.setAll(sorted);
        countLabel.setText(sorted.size() + " Termine");
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
}
