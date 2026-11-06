package com.aniplato.controller;

import com.aniplato.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
 * Photo tiles: a dashed "Add photo" tile, a rounded preview for each chosen picture with a small ✕ to take it out,
 * and one grey tile for the next picture, until the limit is reached. Each pick adds to the photos already chosen.
 * Holds the chosen files; the service checks them (1 to 5).
 */
public class PhotoPicker extends VBox {

    private static final double TILE = 88;

    private final Label status = new Label();
    private final FlowPane tiles = new FlowPane(12, 12);
    private final List<File> files = new ArrayList<>();
    private String note = "";

    public PhotoPicker() {
        super(8);
        status.getStyleClass().add("text-muted");
        status.setWrapText(true);
        setPadding(Insets.EMPTY);
        getChildren().addAll(tiles, status);
        refresh();
    }

    /** The photos chosen so far, in the order they were added. */
    public List<File> files() {
        return List.copyOf(files);
    }

    /**
     * Adds the pictures to those already chosen. A picture that is already there is ignored; once there are
     * {@link Settings#MAX_PHOTOS} the rest are left out. Returns how many were left out for that reason.
     */
    public int add(List<File> picked) {
        int leftOut = 0;
        for (File file : picked) {
            if (files.stream().anyMatch(have -> have.getAbsoluteFile().equals(file.getAbsoluteFile()))) continue;
            if (files.size() >= Settings.MAX_PHOTOS) {
                leftOut++;
                continue;
            }
            files.add(file);
        }
        note = leftOut == 0 ? "" : " " + leftOut + (leftOut == 1 ? " was" : " were") + " not added: at most " + Settings.MAX_PHOTOS + " photos.";
        refresh();
        return leftOut;
    }

    /** Takes one picture out. */
    public void remove(File file) {
        files.removeIf(have -> have.getAbsoluteFile().equals(file.getAbsoluteFile()));
        note = "";
        refresh();
    }

    private void refresh() {
        tiles.getChildren().clear();
        boolean room = files.size() < Settings.MAX_PHOTOS;
        if (room) tiles.getChildren().add(addTile());
        for (File file : files) tiles.getChildren().add(preview(file));
        if (room) tiles.getChildren().add(blank());   // always one grey tile for the next picture
        int count = files.size();
        status.setText((count == 0 ? "No photos selected." : count + (count == 1 ? " photo selected." : " photos selected.")) + note);
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

    private static StackPane blank() {
        StackPane blank = new StackPane();
        blank.getStyleClass().add("photo-blank");
        blank.setMinSize(TILE, TILE);
        blank.setPrefSize(TILE, TILE);
        return blank;
    }

    /** The picture cropped to a square (not stretched), with a ✕ in the corner. */
    private StackPane preview(File file) {
        Image image = new Image(file.toURI().toString(), TILE * 2, TILE * 2, true, true, false);
        StackPane tile = new StackPane();
        tile.setMinSize(TILE, TILE);
        tile.setPrefSize(TILE, TILE);
        tile.setMaxSize(TILE, TILE);
        if (image.isError() || image.getWidth() == 0) {
            tile.getStyleClass().add("photo-blank");
        } else {
            double side = Math.min(image.getWidth(), image.getHeight());
            ImageView view = new ImageView(image);
            view.setViewport(new Rectangle2D((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side));
            view.setFitWidth(TILE);
            view.setFitHeight(TILE);
            Rectangle round = new Rectangle(TILE, TILE);
            round.setArcWidth(16);
            round.setArcHeight(16);
            view.setClip(round);
            tile.getChildren().add(view);
        }
        Button remove = new Button("✕");
        remove.getStyleClass().add("photo-remove");
        remove.setAccessibleText("Remove " + file.getName());
        remove.setOnAction(e -> remove(file));
        StackPane.setAlignment(remove, Pos.TOP_RIGHT);
        StackPane.setMargin(remove, new Insets(4));
        tile.getChildren().add(remove);
        return tile;
    }

    private void choose() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose photos (" + Settings.MIN_PHOTOS + " to " + Settings.MAX_PHOTOS + " in all)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG or PNG", "*.jpg", "*.jpeg", "*.png"));
        Window window = getScene() == null ? null : getScene().getWindow();
        List<File> picked = chooser.showOpenMultipleDialog(window);
        if (picked != null && !picked.isEmpty()) add(picked);
    }
}
