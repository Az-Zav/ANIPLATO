package com.aniplato.controller;

import com.aniplato.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Photo tiles: a dashed "Add photo" tile, a rounded tile for each chosen picture, grey tiles until there are four,
 * and a count line. Holds the chosen files; the service checks them (1 to 5).
 */
public class PhotoPicker extends VBox {

    private static final double TILE = 88;
    private static final int MIN_TILES = 4;

    private final Label status = new Label("No photos selected.");
    private final FlowPane tiles = new FlowPane(12, 12);
    private List<File> files = new ArrayList<>();

    public PhotoPicker() {
        super(8);
        status.getStyleClass().add("text-muted");
        status.setWrapText(true);
        setPadding(Insets.EMPTY);
        getChildren().addAll(tiles, status);
        refresh();
    }

    /** The photos chosen so far. */
    public List<File> files() {
        return List.copyOf(files);
    }

    private void refresh() {
        tiles.getChildren().clear();
        tiles.getChildren().add(addTile());
        for (File file : files) tiles.getChildren().add(picture(file));
        for (int i = files.size() + 1; i < MIN_TILES; i++) {
            StackPane blank = new StackPane();
            blank.getStyleClass().add("photo-blank");
            blank.setMinSize(TILE, TILE);
            blank.setPrefSize(TILE, TILE);
            tiles.getChildren().add(blank);
        }
    }

    private Button addTile() {
        Label plus = new Label("+");
        plus.getStyleClass().add("photo-plus");
        Label text = new Label("Add photo");
        text.getStyleClass().add("photo-add-text");
        VBox inside = new VBox(2, plus, text);
        inside.setAlignment(Pos.CENTER);
        Button add = new Button();
        add.setGraphic(inside);
        add.getStyleClass().add("photo-add");
        add.setMinSize(TILE, TILE);
        add.setPrefSize(TILE, TILE);
        add.setOnAction(e -> choose());
        return add;
    }

    private static StackPane picture(File file) {
        ImageView view = new ImageView(new Image(file.toURI().toString(), TILE, TILE, false, true, true));
        view.setFitWidth(TILE);
        view.setFitHeight(TILE);
        Rectangle round = new Rectangle(TILE, TILE);
        round.setArcWidth(16);
        round.setArcHeight(16);
        view.setClip(round);
        StackPane tile = new StackPane(view);
        tile.setMinSize(TILE, TILE);
        tile.setPrefSize(TILE, TILE);
        return tile;
    }

    private void choose() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose " + Settings.MIN_PHOTOS + " to " + Settings.MAX_PHOTOS + " photos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG or PNG", "*.jpg", "*.jpeg", "*.png"));
        Window window = getScene() == null ? null : getScene().getWindow();
        List<File> picked = chooser.showOpenMultipleDialog(window);
        if (picked == null || picked.isEmpty()) return;
        if (picked.size() > Settings.MAX_PHOTOS) {
            status.setText("Choose no more than " + Settings.MAX_PHOTOS + " photos.");
            return;
        }
        files = new ArrayList<>(picked);
        refresh();
        status.setText(files.size() + (files.size() == 1 ? " photo selected." : " photos selected."));
    }
}
