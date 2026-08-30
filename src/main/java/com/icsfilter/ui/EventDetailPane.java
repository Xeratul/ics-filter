package com.icsfilter.ui;

import com.icsfilter.model.CalendarEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows every detail of a selected event. Sits below the calendar and updates
 * whenever an event is clicked in the calendar or the event list.
 */
public final class EventDetailPane extends ScrollPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");

    private final Label title = new Label();
    private final Label fromValue = new Label();
    private final Label toValue = new Label();
    private final Label categoryValue = new Label();
    private final Label sourceValue = new Label();
    private final TextFlow locationValue = new TextFlow();
    private final TextFlow descriptionValue = new TextFlow();
    private final Label uidValue = new Label();
    private final Label placeholder = new Label("Kein Termin ausgewählt");
    private final VBox details = new VBox();
    private final StackPane root = new StackPane();

    public EventDetailPane() {
        setFitToWidth(true);
        setStyle("-fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;");
        setPannable(true);

        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");
        title.setWrapText(true);

        placeholder.setStyle("-fx-text-fill: #888; -fx-font-size: 14; -fx-padding: 8;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(4);
        grid.setPadding(new Insets(8));
        ColumnConstraints nameCol = new ColumnConstraints();
        nameCol.setMinWidth(100);
        nameCol.setPrefWidth(110);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCol, valueCol);

        addRow(grid, 0, "Von", fromValue);
        addRow(grid, 1, "Bis", toValue);
        addRow(grid, 2, "Kategorie", categoryValue);
        addRow(grid, 3, "Quelle", sourceValue);
        addRow(grid, 4, "Ort", locationValue);
        addRow(grid, 5, "Beschreibung", descriptionValue);
        addRow(grid, 6, "UID", uidValue);

        locationValue.setMaxWidth(Double.MAX_VALUE);
        descriptionValue.setMaxWidth(Double.MAX_VALUE);

        VBox wrap = new VBox(title, grid);
        wrap.setPadding(new Insets(6));
        details.getChildren().add(wrap);

        root.getChildren().addAll(placeholder, details);
        setContent(root);
        setEvent(null);
    }

    /** Displays the given event, or a placeholder when {@code event} is null. */
    public void setEvent(CalendarEvent event) {
        if (event == null) {
            toggle(true);
            return;
        }

        toggle(false);
        title.setText(event.summary());
        fromValue.setText(spanStart(event));
        toValue.setText(spanEnd(event));
        categoryValue.setText(emptyOr(event.category()));
        sourceValue.setText(event.source() == null ? "—" : event.source().name());
        setFlow(locationValue, event.location());
        setFlow(descriptionValue, event.description());
        uidValue.setText(emptyOr(event.uid()));
    }

    private String spanStart(CalendarEvent e) {
        if (e.startDate() == null) {
            return "—";
        }
        if (e.allDay()) {
            return e.startDate().format(DATE_FMT);
        }
        return e.start() == null ? e.startDate().format(DATE_FMT) : e.start().format(DATETIME_FMT);
    }

    private String spanEnd(CalendarEvent e) {
        if (e.end() == null) {
            return "—";
        }
        if (e.allDay()) {
            return e.end().toLocalDate().format(DATE_FMT);
        }
        return e.end().format(DATETIME_FMT);
    }

    private String emptyOr(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    /** Fills a TextFlow with plain text and clickable hyperlinks for any URLs. */
    private void setFlow(TextFlow flow, String text) {
        flow.getChildren().clear();
        if (text == null || text.isBlank()) {
            flow.getChildren().add(new Text("—"));
            return;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                flow.getChildren().add(new Text(text.substring(last, matcher.start())));
            }
            String url = stripTrailingPunctuation(matcher.group());
            Hyperlink link = new Hyperlink(url);
            link.setOnAction(e -> openUrl(url));
            flow.getChildren().add(link);
            last = matcher.end();
        }
        if (last < text.length()) {
            flow.getChildren().add(new Text(text.substring(last)));
        }
    }

    private String stripTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ".,;!?:)]}'\"".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ex) {
            System.err.println("Could not open " + url + ": " + ex.getMessage());
        }
    }

    private void toggle(boolean showPlaceholder) {
        placeholder.setVisible(showPlaceholder);
        placeholder.setManaged(showPlaceholder);
        details.setVisible(!showPlaceholder);
        details.setManaged(!showPlaceholder);
    }

    private void addRow(GridPane grid, int row, String name, Node value) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #666;");
        grid.add(nameLabel, 0, row);
        grid.add(value, 1, row);
    }
}
