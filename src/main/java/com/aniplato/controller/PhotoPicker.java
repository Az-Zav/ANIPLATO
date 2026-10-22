package com.aniplato.controller;

import com.aniplato.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** "Add photos" button, the previews and a count line. Holds the chosen files; the service checks them (1 to 5). */
public class PhotoPicker extends VBox {

    private final Label status = new Label("No photos selected.");
    private final FlowPane previews = new FlowPane(12, 12);
    private List<File> files = new ArrayList<>();

    public PhotoPicker() {
        super(8);
        Button add = new Button("Add photos");
        add.getStyleClass().add("secondary-button");
        add.setOnAction(e -> choose());
        status.getStyleClass().add("text-muted");
        status.setWrapText(true);
        setPadding(Insets.EMPTY);
        getChildren().addAll(add, previews, status);
    }

    /** The photos chosen so far. */
    public List<File> files() {
        return List.copyOf(files);
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
        previews.getChildren().clear();
        for (File file : files) {
            ImageView view = new ImageView(new Image(file.toURI().toString(), 96, 96, true, true, true));
            Label name = new Label(file.getName());
            name.getStyleClass().add("text-muted");
            name.setMaxWidth(96);
            VBox item = new VBox(4, view, name);
            item.setAlignment(Pos.TOP_CENTER);
            previews.getChildren().add(item);
        }
        status.setText(files.size() + (files.size() == 1 ? " photo selected." : " photos selected."));
    }
}
