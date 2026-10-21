package com.aniplato.controller.donor;

import com.aniplato.util.Navigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class MakeOfferController {

    @FXML private TextField itemDescriptionField;
    @FXML private TextField quantityField;
    @FXML private TextField safeUseByField;
    @FXML private TextField pickupWindowField;
    @FXML private TextField handoverInstructionsField;
    @FXML private CheckBox donorAttestationCheck;
    @FXML private Label photoStatusLabel;
    @FXML private Label formStatusLabel;
    @FXML private RadioButton ambientDryRadio;
    @FXML private RadioButton chilledRadio;
    @FXML private RadioButton hotHoldRadio;

    private List<File> selectedPhotos = List.of();

    @FXML
    private void initialize() {
        ToggleGroup storageGroup = new ToggleGroup();
        ambientDryRadio.setToggleGroup(storageGroup);
        chilledRadio.setToggleGroup(storageGroup);
        hotHoldRadio.setToggleGroup(storageGroup);
        ambientDryRadio.setSelected(true);
        photoStatusLabel.setText("0 of 5 photos selected.");
        formStatusLabel.setText("");
    }

    // PATTERN:NAV -> DONOR_BROWSE_REQUESTS
    @FXML
    private void handleBackToRequests() {
        Navigator.show(Navigator.Screen.DONOR_BROWSE_REQUESTS);
    }

    // PATTERN:ACTION handleAddPhotos
    @FXML
    private void handleAddPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add food photos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        Window window = photoStatusLabel.getScene() == null ? null : photoStatusLabel.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(window);
        if (files == null || files.isEmpty()) {
            return;
        }

        if (selectedPhotos.size() + files.size() > 5) {
            formStatusLabel.setText("You can add up to five photos.");
            return;
        }

        selectedPhotos = FXCollections.observableArrayList(selectedPhotos);
        selectedPhotos.addAll(files);
        photoStatusLabel.setText(selectedPhotos.size() + " of 5 photos selected.");
        formStatusLabel.setText("");
    }

    // PATTERN:ACTION handleSendOffer
    @FXML
    private void handleSendOffer() {
        if (itemDescriptionField.getText().isBlank()
                || quantityField.getText().isBlank()
                || safeUseByField.getText().isBlank()
                || pickupWindowField.getText().isBlank()
                || !donorAttestationCheck.isSelected()) {
            formStatusLabel.setText("Complete the required fields and donor attestation before sending.");
            return;
        }

        // TODO(service): submit the offer through the offer service (FLOWS.md module 6)
        formStatusLabel.setText("Offer sent successfully.");
    }

    // PATTERN:ACTION handleCancel
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.DONOR_BROWSE_REQUESTS);
    }
}
