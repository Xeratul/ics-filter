package com.icsfilter.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Filters the visible events by keyword, date range and category.
 */
public final class FilterPane extends VBox {

    private final TextField keywordField = new TextField();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final ObservableList<String> categories = FXCollections.observableArrayList();
    private final ListView<String> categoryList = new ListView<>(categories);
    private final Set<String> selectedCategories = new HashSet<>();

    private Runnable onChange = () -> { };

    public FilterPane() {
        setSpacing(8);
        setPadding(new Insets(10));

        Label title = new Label("Filter");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(title);

        keywordField.setPromptText("Stichwort");
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        keywordField.textProperty().addListener((obs, o, n) -> onChange.run());

        HBox dateRow = new HBox(6);
        fromPicker.setPromptText("Von");
        toPicker.setPromptText("Bis");
        HBox.setHgrow(fromPicker, Priority.ALWAYS);
        HBox.setHgrow(toPicker, Priority.ALWAYS);
        dateRow.getChildren().addAll(fromPicker, toPicker);
        fromPicker.valueProperty().addListener((obs, o, n) -> onChange.run());
        toPicker.valueProperty().addListener((obs, o, n) -> onChange.run());

        Button reset = new Button("Zurücksetzen");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(e -> reset());

        categoryList.setPrefHeight(120);
        categoryList.setCellFactory(lv -> new CategoryCell());

        getChildren().addAll(new Label("Stichwort"), keywordField, new Label("Zeitraum"), dateRow,
                new Label("Kategorien"), categoryList, reset);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public String keyword() {
        return keywordField.getText();
    }

    public LocalDate from() {
        return fromPicker.getValue();
    }

    public LocalDate to() {
        return toPicker.getValue();
    }

    public Set<String> selectedCategories() {
        return selectedCategories;
    }

    /** Restores the keyword field (does not trigger a re-filter). */
    public void keyword(String value) {
        keywordField.setText(value);
    }

    /** Restores the "from" date (does not trigger a re-filter). */
    public void from(LocalDate value) {
        fromPicker.setValue(value);
    }

    /** Restores the "to" date (does not trigger a re-filter). */
    public void to(LocalDate value) {
        toPicker.setValue(value);
    }

    /** Restores the selected category names (does not trigger a re-filter). */
    public void selectCategories(java.util.Collection<String> values) {
        selectedCategories.clear();
        selectedCategories.addAll(values);
        categoryList.refresh();
    }

    /** Refreshes the category check boxes from the currently loaded events. */
    public void setCategories(java.util.Collection<String> allCategories) {
        categories.setAll(new TreeSet<>(allCategories));
        categoryList.refresh();
    }

    public void reset() {
        keywordField.clear();
        fromPicker.setValue(null);
        toPicker.setValue(null);
        selectedCategories.clear();
        categoryList.refresh();
        onChange.run();
    }

    private final class CategoryCell extends ListCell<String> {
        private final CheckBox checkBox = new CheckBox();
        private String category;

        CategoryCell() {
            setGraphic(checkBox);
            checkBox.setOnAction(e -> {
                if (category != null) {
                    if (checkBox.isSelected()) {
                        selectedCategories.add(category);
                    } else {
                        selectedCategories.remove(category);
                    }
                    onChange.run();
                }
            });
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            category = value;
            if (empty || value == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setGraphic(checkBox);
            checkBox.setText(value);
            checkBox.setSelected(selectedCategories.contains(value));
        }
    }
}
