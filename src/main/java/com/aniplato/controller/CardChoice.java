package com.aniplato.controller;

import com.aniplato.model.FoodDetail;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Cards of which at most one is chosen, each with a title and a line under it (the Storage condition). Used in FXML
 * as one control; the controller fills it with {@link #setItems}.
 */
public class CardChoice<T> extends HBox {

    private final ToggleGroup group = new ToggleGroup();
    private final List<T> items = new ArrayList<>();

    public CardChoice() {
        super(12);
        getStyleClass().add("choice-row");
    }

    /** Shows one card per item, none chosen. */
    public void setItems(List<T> choices, Function<T, String> title, Function<T, String> hint) {
        items.clear();
        items.addAll(choices);
        getChildren().clear();
        for (T item : choices) {
            Label name = new Label(title.apply(item));
            name.getStyleClass().add("choice-card-title");
            Label line = new Label(hint.apply(item));
            line.getStyleClass().add("choice-card-hint");
            line.setWrapText(true);
            VBox text = new VBox(4, name, line);
            ToggleButton card = new ToggleButton();
            card.setGraphic(text);
            card.getStyleClass().add("choice-card");
            card.setAlignment(Pos.TOP_LEFT);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setToggleGroup(group);
            card.setUserData(item);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setPrefWidth(260);   // a real width so the hint wraps at card height, then the cards share the row equally
            card.setMinWidth(0);
            card.setPrefHeight(78);   // two lines of hint; a card is never taller than that
            card.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            getChildren().add(card);
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

    /** The line under each storage condition, as in the design. */
    public static String storageHint(FoodDetail.Storage storage) {
        return switch (storage) {
            case AMBIENT_DRY -> "Room temperature (22–28 °C), sealed containers.";
            case CHILLED -> "Kept at 4 °C or below. Bring an insulated cooler.";
            case HOT_HOLD -> "Kept above 60 °C. Pick up within 90 minutes.";
        };
    }
}
