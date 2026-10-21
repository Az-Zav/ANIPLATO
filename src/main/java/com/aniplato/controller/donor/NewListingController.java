package com.aniplato.controller.donor;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class NewListingController {

    @FXML private TextField foodDescriptionField;
    @FXML private TextField quantityField;
    @FXML private DatePicker useByDatePicker;
    @FXML private TextField pickupWindowField;
    @FXML private HBox photoRowBox;
    @FXML private Label photoStatusLabel;

    @FXML
    private void initialize() {
        // TODO(service): populate categoryComboBox from listingService.getCategories()
        // TODO(service): submit listing details to listingService.createListing(...) -- R1.1
    }

    // PATTERN:ACTION opens an image-only multi-file picker
    @FXML
    private void handleAddPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select food photos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        Window window = photoRowBox.getScene() == null ? null : photoRowBox.getScene().getWindow();
        List<File> selected = chooser.showOpenMultipleDialog(window);
        if (selected == null || selected.isEmpty()) return;
        if (selected.size() > 5) {
            photoStatusLabel.setText("Select no more than 5 photos.");
            return;
        }

        photoRowBox.getChildren().clear();
        for (File file : selected) {
            ImageView preview = new ImageView(new Image(file.toURI().toString(), 96, 96, true, true, true));
            preview.setPreserveRatio(true);
            preview.setFitWidth(96);
            preview.setFitHeight(96);

            Label name = new Label(file.getName());
            name.getStyleClass().add("text-muted");
            name.setMaxWidth(96);
            name.setWrapText(true);

            VBox photo = new VBox(4, preview, name);
            photo.setAlignment(Pos.TOP_CENTER);
            photoRowBox.getChildren().add(photo);
        }
        photoStatusLabel.setText(selected.size() + (selected.size() == 1 ? " photo selected." : " photos selected."));
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handlePublish() {
        Navigator.show(Navigator.Screen.MY_LISTINGS);
    }
}
