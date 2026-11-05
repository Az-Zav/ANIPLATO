package com.aniplato.controller;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A row of chips of which at most one is chosen (the Category of a listing or an offer). It replaces the drop-down so
 * every choice is visible at once. Used in FXML as one control; the controller fills it with {@link #setItems}.
 */
public class ChipChoice<T> extends FlowPane {

    private final ToggleGroup group = new ToggleGroup();
    private final List<T> items = new ArrayList<>();

    public ChipChoice() {
        super(8, 8);
        getStyleClass().add("chip-row");
    }

    /** Shows one chip per item, none chosen. */
    public void setItems(List<T> choices, Function<T, String> label) {
        items.clear();
        items.addAll(choices);
        getChildren().clear();
        for (T item : choices) {
            ToggleButton chip = new ToggleButton(label.apply(item));
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(group);
            chip.setUserData(item);
            getChildren().add(chip);
        }
    }

    public List<T> getItems() {
        return List.copyOf(items);
    }

    /** The chosen item, or null when none is chosen. */
    @SuppressWarnings("unchecked")
    public T getValue() {
        return group.getSelectedToggle() == null ? null : (T) group.getSelectedToggle().getUserData();
    }

    public void setValue(T value) {
        for (var toggle : group.getToggles()) {
            if (toggle.getUserData().equals(value)) {
                toggle.setSelected(true);
                return;
            }
        }
        if (group.getSelectedToggle() != null) group.getSelectedToggle().setSelected(false);
    }
}
