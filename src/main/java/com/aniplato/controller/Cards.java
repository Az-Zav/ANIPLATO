package com.aniplato.controller;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Small builders for the card rows the list screens fill in code. No rules here: only layout and the app's style classes. */
public final class Cards {

    private Cards() { }

    /** A bordered card holding the given rows. */
    public static VBox card(Node... children) {
        VBox card = new VBox(6, children);
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(16));
        return card;
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        label.setWrapText(true);
        return label;
    }

    public static Label line(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    public static Label muted(String text) {
        Label label = line(text);
        label.getStyleClass().add("text-muted");
        return label;
    }

    public static Button primary(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    public static Button secondary(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    /** A row of buttons; an empty row takes no space. */
    public static HBox actions(Node... buttons) {
        HBox row = new HBox(8, buttons);
        row.setManaged(buttons.length > 0);
        row.setVisible(buttons.length > 0);
        return row;
    }

    /** Puts the rows into the box, or shows the empty-state label when there are none. */
    public static void fill(VBox box, Label emptyLabel, List<? extends Node> rows) {
        box.getChildren().setAll(rows);
        emptyLabel.setVisible(rows.isEmpty());
        emptyLabel.setManaged(rows.isEmpty());
    }

    /** Adds each line as a muted label under the card's heading. */
    public static void addLines(VBox card, List<String> lines) {
        for (String text : lines) card.getChildren().add(muted(text));
    }
}
