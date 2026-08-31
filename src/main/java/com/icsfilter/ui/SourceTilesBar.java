package com.icsfilter.ui;

import com.icsfilter.model.CalendarSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

/**
 * Shows the calendar sources as horizontal tiles in a top bar. Sources are
 * toggled on/off by clicking a tile, edited via the small corner button and
 * added through the trailing "+ neue Quelle" tile. Adding and editing happens
 * in a dialog.
 */
public final class SourceTilesBar extends VBox {

    private final ObservableList<CalendarSource> sources = FXCollections.observableArrayList();
    private final Set<String> enabled = new HashSet<>();

    private final HBox tilesBox = new HBox(10);
    private Runnable onSourcesChanged = () -> { };

    public SourceTilesBar() {
        setPadding(new Insets(8));

        tilesBox.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(tilesBox);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;"); 
        getChildren().add(scroll);

        rebuild();
    }

    public ObservableList<CalendarSource> sources() {
        return sources;
    }

    public Set<String> enabled() {
        return enabled;
    }

    public void setOnSourcesChanged(Runnable onSourcesChanged) {
        this.onSourcesChanged = onSourcesChanged;
    }

    /** Rebuilds the tiles (e.g. after restoring persisted state). */
    public void refresh() {
        rebuild();
    }

    // ------------------------------------------------------------------
    // Tile building
    // ------------------------------------------------------------------

    private void rebuild() {
        tilesBox.getChildren().clear();
        for (int i = 0; i < sources.size(); i++) {
            tilesBox.getChildren().add(sourceTile(sources.get(i)));
        }
        tilesBox.getChildren().add(addTile());
    }

    private StackPane sourceTile(CalendarSource source) {
        int index = sources.indexOf(source);
        Color color = UiPalette.colorFor(index < 0 ? 0 : index);
        boolean on = enabled.contains(source.name());

        Label name = new Label(source.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        name.setMaxWidth(150);
        name.setWrapText(true);

        Label url = new Label(source.url());
        url.setStyle("-fx-font-size: 9; -fx-text-fill: #777;");
        url.setMaxWidth(150);
        url.setWrapText(true);

        VBox content = new VBox(2, name, url);
        if (!source.filter().isBlank()) {
            Label filter = new Label("enth\u00e4lt: " + source.filter());
            filter.setStyle("-fx-font-size: 9; -fx-text-fill: #2a9d8f;");
            filter.setMaxWidth(150);
            filter.setWrapText(true);
            content.getChildren().add(filter);
        }
        content.setPadding(new Insets(8, 10, 8, 10));

        Button edit = new Button("\u270e");
        edit.setFocusTraversable(false);
        edit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 2 0 2;");
        edit.setOnAction(e -> editSource(source));

        StackPane tile = new StackPane(content, edit);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        StackPane.setAlignment(edit, Pos.TOP_RIGHT);
        StackPane.setMargin(edit, new Insets(6));
        tile.setPrefWidth(180);
        tile.setMinWidth(170);
        tile.setPrefHeight(82);
        tile.setMinHeight(70);
        tile.setOnMouseClicked(e -> toggle(source));
        applyTileStyle(tile, color, on);
        return tile;
    }

    private StackPane addTile() {
        Label plus = new Label("+ neue Quelle");
        plus.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #2a9d8f;");
        plus.setPadding(new Insets(8, 12, 8, 12));

        StackPane tile = new StackPane(plus);
        tile.setPrefWidth(150);
        tile.setMinWidth(150);
        tile.setPrefHeight(82);
        tile.setCursor(Cursor.HAND);
        tile.setStyle("-fx-background-color: #f4faf9; -fx-border-color: #2a9d8f; "
                + "-fx-border-width: 1; -fx-border-style: dashed; "
                + "-fx-background-radius: 8; -fx-border-radius: 8;");
        tile.setOnMouseClicked(e -> openAddDialog());
        return tile;
    }

    private void applyTileStyle(StackPane tile, Color color, boolean on) {
        if (on) {
            tile.setOpacity(1.0);
            tile.setStyle("-fx-background-color: #ffffff; -fx-border-color: " + toCss(color)
                    + "; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");
        } else {
            tile.setOpacity(0.6);
            tile.setStyle("-fx-background-color: #efefef; -fx-border-color: #c8c8c8; "
                    + "-fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");
        }
    }

    private void toggle(CalendarSource source) {
        if (enabled.contains(source.name())) {
            enabled.remove(source.name());
        } else {
            enabled.add(source.name());
        }
        onSourcesChanged.run();
        rebuild();
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    private void openAddDialog() {
        TextField name = new TextField();
        TextField url = new TextField();
        TextField filter = new TextField();
        Dialog<CalendarSource> dialog = new Dialog<>();
        dialog.setTitle("Neue Quelle");
        ButtonType save = new ButtonType("Hinzufügen", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);
        dialog.getDialogPane().setContent(formGrid(name, url, filter));
        dialog.getDialogPane().setPrefWidth(380);
        wireValidation(dialog.getDialogPane(), save, name, url);
        dialog.setResultConverter(bt -> {
            if (bt == save) {
                String n = name.getText().trim();
                String u = url.getText().trim();
                if (n.isEmpty() || u.isEmpty()) {
                    return null;
                }
                return new CalendarSource(n, u, filter.getText().trim());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(source -> {
            sources.add(source);
            enabled.add(source.name());
            onSourcesChanged.run();
            rebuild();
        });
    }

    private void editSource(CalendarSource original) {
        TextField name = new TextField(original.name());
        TextField url = new TextField(original.url());
        TextField filter = new TextField(original.filter());
        Dialog<DialogResult> dialog = new Dialog<>();
        dialog.setTitle("Quelle bearbeiten");
        ButtonType save = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType delete = new ButtonType("Entfernen", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, delete, cancel);
        dialog.getDialogPane().setContent(formGrid(name, url, filter));
        dialog.getDialogPane().setPrefWidth(380);
        wireValidation(dialog.getDialogPane(), save, name, url);
        dialog.setResultConverter(bt -> {
            if (bt == save) {
                String n = name.getText().trim();
                String u = url.getText().trim();
                if (n.isEmpty() || u.isEmpty()) {
                    return null;
                }
                return new DialogResult(new CalendarSource(n, u, filter.getText().trim()), false);
            }
            if (bt == delete) {
                return new DialogResult(null, true);
            }
            return null;
        });
        dialog.showAndWait().ifPresent(result -> {
            if (result.delete()) {
                sources.remove(original);
                enabled.remove(original.name());
            } else if (result.source() != null) {
                int idx = sources.indexOf(original);
                CalendarSource updated = result.source();
                if (idx >= 0) {
                    sources.set(idx, updated);
                }
                if (enabled.contains(original.name())) {
                    enabled.remove(original.name());
                    enabled.add(updated.name());
                }
            }
            onSourcesChanged.run();
            rebuild();
        });
    }

    private GridPane formGrid(TextField name, TextField url, TextField filter) {
        name.setPromptText("Name");
        url.setPromptText("https://.../calendar.ics");
        filter.setPromptText("Filter (im Titel)");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Name"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("URL"), 0, 1);
        grid.add(url, 1, 1);
        grid.add(new Label("Filter"), 0, 2);
        grid.add(filter, 1, 2);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(url, Priority.ALWAYS);
        GridPane.setHgrow(filter, Priority.ALWAYS);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private void wireValidation(DialogPane pane, ButtonType ok, TextField name, TextField url) {
        Node okButton = pane.lookupButton(ok);
        Runnable update = () -> okButton.setDisable(name.getText().trim().isEmpty()
                || url.getText().trim().isEmpty());
        update.run();
        name.textProperty().addListener((obs, o, n) -> update.run());
        url.textProperty().addListener((obs, o, n) -> update.run());
    }

    private String toCss(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    /** Result of the edit dialog: either a new source or a delete request. */
    private record DialogResult(CalendarSource source, boolean delete) {
    }
}
