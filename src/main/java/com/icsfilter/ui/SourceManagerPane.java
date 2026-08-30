package com.icsfilter.ui;

import com.icsfilter.model.CalendarSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

/**
 * Lets the user add, remove, enable/disable and refresh the ICS sources.
 */
public final class SourceManagerPane extends VBox {

    private final ObservableList<CalendarSource> sources = FXCollections.observableArrayList();
    private final Set<String> enabled = new HashSet<>();
    private final ListView<CalendarSource> listView = new ListView<>(sources);

    private final TextField nameField = new TextField();
    private final TextField urlField = new TextField();

    private Runnable onReload = () -> { };
    private Runnable onSourcesChanged = () -> { };

    public SourceManagerPane() {
        setSpacing(8);
        setPadding(new Insets(10));

        Label title = new Label("Kalenderquellen");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(title);

        listView.setPrefHeight(160);
        listView.setCellFactory(lv -> new SourceCell());
        getChildren().add(listView);

        HBox addRow1 = new HBox(6);
        nameField.setPromptText("Name");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        addRow1.getChildren().add(nameField);

        HBox addRow2 = new HBox(6);
        urlField.setPromptText("https://.../calendar.ics");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        addRow2.getChildren().add(urlField);

        Button addButton = new Button("Hinzufügen");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addCurrent());

        Button removeButton = new Button("Entfernen");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setOnAction(e -> removeSelected());

        Button reloadButton = new Button("Aktualisieren");
        reloadButton.setMaxWidth(Double.MAX_VALUE);
        reloadButton.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;");
        reloadButton.setOnAction(e -> onReload.run());

        getChildren().addAll(addRow1, addRow2, addButton);
        getChildren().add(new HBox(6, removeButton, reloadButton));
    }

    public ObservableList<CalendarSource> sources() {
        return sources;
    }

    public Set<String> enabled() {
        return enabled;
    }

    public void setOnReload(Runnable onReload) {
        this.onReload = onReload;
    }

    public void setOnSourcesChanged(Runnable onSourcesChanged) {
        this.onSourcesChanged = onSourcesChanged;
    }

    private void addCurrent() {
        String name = nameField.getText().trim();
        String url = urlField.getText().trim();
        if (name.isEmpty() || url.isEmpty()) {
            return;
        }
        sources.add(new CalendarSource(name, url));
        enabled.add(name);
        nameField.clear();
        urlField.clear();
        onSourcesChanged.run();
        listView.refresh();
    }

    private void removeSelected() {
        CalendarSource selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            sources.remove(selected);
            enabled.remove(selected.name());
            onSourcesChanged.run();
            listView.refresh();
        }
    }

    private final class SourceCell extends ListCell<CalendarSource> {
        private final CheckBox checkBox = new CheckBox();
        private final Label nameLabel = new Label();
        private CalendarSource item;

        SourceCell() {
            HBox box = new HBox(6);
            box.setAlignment(Pos.CENTER_LEFT);
            checkBox.setOnAction(e -> {
                if (item != null) {
                    if (checkBox.isSelected()) {
                        enabled.add(item.name());
                    } else {
                        enabled.remove(item.name());
                    }
                    onSourcesChanged.run();
                }
            });
            box.getChildren().addAll(checkBox, nameLabel);
            setGraphic(box);
        }

        @Override
        protected void updateItem(CalendarSource source, boolean empty) {
            super.updateItem(source, empty);
            item = source;
            if (empty || source == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setGraphic(getGraphic());
            nameLabel.setText(source.name());
            Color color = UiPalette.colorFor(indexOfSources(source));
            checkBox.setTextFill(color);
            checkBox.setSelected(enabled.contains(source.name()));
            checkBox.setText("");
            setStyle("-fx-background-color: " + toCss(color.deriveColor(0, 1, 1, 0.25)) + ";");
        }

        private int indexOfSources(CalendarSource source) {
            return sources.indexOf(source) < 0 ? 0 : sources.indexOf(source);
        }

        private String toCss(Color color) {
            return String.format("#%02x%02x%02x",
                    (int) Math.round(color.getRed() * 255),
                    (int) Math.round(color.getGreen() * 255),
                    (int) Math.round(color.getBlue() * 255));
        }
    }
}
