package com.aniplato.controller.board;

import com.aniplato.util.SvgIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Builds the row of summary tiles. Layout and style classes only. */
public final class Tiles {

    private Tiles() { }

    public static HBox row(List<Tile> tiles) {
        HBox row = new HBox(16);
        for (Tile tile : tiles) {
            VBox box = build(tile);
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);
            box.setPrefWidth(0);   // equal widths: the tiles share the row
            row.getChildren().add(box);
        }
        return row;
    }

    private static VBox build(Tile tile) {
        StackPane chip = new StackPane(SvgIcon.load(tile.icon().file, 18));
        chip.getStyleClass().addAll("board-chip", tile.icon().style);
        Label title = new Label(tile.title());
        title.getStyleClass().add("stat-card-title");
        title.setWrapText(true);
        HBox top = new HBox(10, chip, title);
        top.setAlignment(Pos.CENTER_LEFT);

        Label value = new Label(tile.value());
        value.getStyleClass().add("dashboard-metric-value");
        HBox figure = new HBox(6, value);
        figure.setAlignment(Pos.BASELINE_LEFT);
        if (tile.unit() != null) {
            Label unit = new Label(tile.unit());
            unit.getStyleClass().add("dashboard-unit");
            figure.getChildren().add(unit);
        }
        Label note = new Label(tile.note());
        note.getStyleClass().add("dashboard-subtitle");
        note.setWrapText(true);

        VBox box = new VBox(12, top, figure, note);
        box.getStyleClass().add("stat-card");
        return box;
    }
}
